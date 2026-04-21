#!/usr/bin/env zsh
# =============================================================================
# local-risking-e2e-test.sh
#
# Single end-to-end local test:
#   1. Cleans any stale / malformed seed data from Mongo
#   2. Seeds ONE valid ApplicationForRisking record via the test-only endpoint
#   3. Captures applicationReference + personReference from Mongo
#   4. Writes a "pass" results file and serves it over a temporary HTTP server
#   5. Patches the SDES stub canned response to point at that file
#   6. Triggers GET /test-only/download-available-results-files
#   7. Verifies the application status in Mongo (expects Approved or SubscribedAndEnrolled)
#
# Prerequisites (all must be running before you execute this script):
#   - agent-registration-risking:
#       cd agent-registration-risking
#       sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run
#   - secure-data-exchange-list-files-stubs:
#       cd secure-data-exchange-list-files-stubs
#       sbt -Dhttp.port=8765 run
#   - MongoDB on localhost:27017
#   - object-store stub on localhost:8464 (or sm2 OBJECT_STORE_STUB)
#   - mongosh, python3, curl  (brew install mongosh python)
# =============================================================================

set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
fail()    { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }
has_cmd() { command -v "$1" >/dev/null 2>&1; }

# ── Config ────────────────────────────────────────────────────────────────────
BASE_URL="http://localhost:22203"
MONGO_DB="agent-registration-risking"
MONGO_COLLECTION="application-for-risking"

RESULTS_FILE_PORT=19999
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
RESULTS_FILE_NAME="risking_results_${RUN_ID}.json"
RESULTS_FILE_PATH="/tmp/${RESULTS_FILE_NAME}"
RESULTS_FILE_URL="http://localhost:${RESULTS_FILE_PORT}/${RESULTS_FILE_NAME}"

SDES_STUB_CANNED_RESPONSE="/Users/markbennett/workspace/secure-data-exchange-list-files-stubs/conf/responses/downloads/apiDownloadFiles.json"

HTTP_SERVER_PID=""

# ── Cleanup on exit ───────────────────────────────────────────────────────────
cleanup() {
  if [[ -n "$HTTP_SERVER_PID" ]] && kill -0 "$HTTP_SERVER_PID" 2>/dev/null; then
    info "Stopping temporary HTTP server (PID $HTTP_SERVER_PID)..."
    kill "$HTTP_SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# ── Step 0: Prerequisites ─────────────────────────────────────────────────────
info "Step 0 – Checking prerequisites..."
for cmd in mongosh python3 curl; do
  has_cmd "$cmd" || fail "'$cmd' is not installed. Install with: brew install $cmd"
done
success "Tools OK."

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/ping/ping" || true)
[[ "$HTTP_STATUS" == "200" ]] || fail "agent-registration-risking not responding (HTTP $HTTP_STATUS). Start with: sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run"
success "Service is up."

curl -s -o /dev/null -w "%{http_code}" "http://localhost:8765/ping/ping" | grep -q "200" \
  || warn "SDES stub may not be running on :8765 – start with: sbt -Dhttp.port=8765 run (in secure-data-exchange-list-files-stubs)"

# ── Step 1: Clean stale / malformed Mongo seed data ──────────────────────────
info "Step 1 – Removing any malformed or stale local-test documents from Mongo..."
DELETED=$(mongosh --quiet "$MONGO_DB" --eval "
  const result = db.getCollection('$MONGO_COLLECTION').deleteMany({
    \$or: [
      { applicationReference: 'LOCAL-TEST-APP-001' },
      { 'individuals.nino.value':               { \$exists: true } },
      { 'individuals.saUtr.value':              { \$exists: true } },
      { 'individuals.providedDateOfBirth.value':{ \$exists: true } }
    ]
  });
  print(result.deletedCount);
")
success "Removed $DELETED stale document(s)."

# ── Step 2: Seed valid application via test-only endpoint ─────────────────────
info "Step 2 – Seeding a fresh ApplicationForRisking (1 individual) via test-only endpoint..."
APP_JSON=$(curl -s "$BASE_URL/agent-registration-risking/test-only/create-application-for-risking/1")
APP_REF=$(echo "$APP_JSON" | python3 -c "import sys,json; print(json.load(sys.stdin)['applicationReference'])" 2>/dev/null \
  || fail "Could not parse applicationReference from: $APP_JSON")
success "Seeded application: $APP_REF"

# ── Step 3: Read back personReference ─────────────────────────────────────────
info "Step 3 – Reading personReference from Mongo..."
PERSON_REF=$(mongosh --quiet "$MONGO_DB" --eval "
  const d = db.getCollection('$MONGO_COLLECTION').findOne(
    { applicationReference: '$APP_REF' },
    { 'individuals.personReference': 1 }
  );
  if (!d || !d.individuals || !d.individuals[0]) { print('NOT_FOUND'); }
  else { print(d.individuals[0].personReference); }
")
[[ "$PERSON_REF" == "NOT_FOUND" ]] && fail "Could not find seeded document for $APP_REF in Mongo."
success "personReference: $PERSON_REF"

# ── Step 4: Write results file (both entity and individual PASS) ──────────────
info "Step 4 – Writing results file to $RESULTS_FILE_PATH..."
cat > "$RESULTS_FILE_PATH" <<EOF
[
  {
    "recordType": "Entity",
    "applicationReference": "$APP_REF",
    "failures": []
  },
  {
    "recordType": "Individual",
    "personReference": "$PERSON_REF",
        "failures": []
  }
]
EOF
FILE_SIZE=$(wc -c < "$RESULTS_FILE_PATH" | tr -d ' ')
success "Results file written ($FILE_SIZE bytes)."

# ── Step 5: Serve the results file ────────────────────────────────────────────
info "Step 5 – Starting temporary HTTP file server on port $RESULTS_FILE_PORT..."
pkill -f "http.server $RESULTS_FILE_PORT" 2>/dev/null || true
sleep 0.5
python3 -m http.server "$RESULTS_FILE_PORT" --directory /tmp &>/tmp/e2e-http-server.log &
HTTP_SERVER_PID=$!
sleep 1
kill -0 "$HTTP_SERVER_PID" 2>/dev/null || fail "Could not start HTTP server on port $RESULTS_FILE_PORT – check it is free."
success "File server running (PID $HTTP_SERVER_PID) → $RESULTS_FILE_URL"

# ── Step 6: Patch SDES stub canned response ───────────────────────────────────
info "Step 6 – Patching SDES stub canned response → $SDES_STUB_CANNED_RESPONSE..."
if [[ ! -f "$SDES_STUB_CANNED_RESPONSE" ]]; then
  warn "Canned response file not found at expected path. You may need to set SDES_STUB_CANNED_RESPONSE manually."
else
  cat > "$SDES_STUB_CANNED_RESPONSE" <<EOF
[
  {
    "id": "5a5f5b7a73d08bacfef342be",
    "filename": "$RESULTS_FILE_NAME",
    "downloadURL": "$RESULTS_FILE_URL",
    "fileSize": $FILE_SIZE
  }
]
EOF
  success "Canned response patched. SDES stub will pick this up on its next request (auto-reload in sbt dev mode)."
  # Wait for the sbt dev-mode classloader to detect the file change and reload
  info "Waiting 5s for SDES stub to reload canned response..."
  sleep 5
fi

# ── Step 7: Trigger processing ────────────────────────────────────────────────
info "Step 7 – Triggering download-available-results-files..."
TRIGGER_RESPONSE=$(curl -s -w "\n%{http_code}" \
  "$BASE_URL/agent-registration-risking/test-only/download-available-results-files")
TRIGGER_BODY=$(echo "$TRIGGER_RESPONSE" | sed '$d')
TRIGGER_STATUS=$(echo "$TRIGGER_RESPONSE" | tail -n 1)

if [[ "$TRIGGER_STATUS" == "200" ]]; then
  success "Endpoint returned HTTP 200. Response: $TRIGGER_BODY"
else
  fail "Endpoint returned HTTP $TRIGGER_STATUS. Body: $TRIGGER_BODY"
fi

# Give async processing a moment to settle
sleep 2

# ── Step 8: Verify Mongo ───────────────────────────────────────────────────────
info "Step 8 – Verifying application status in Mongo..."
RESULT=$(mongosh --quiet "$MONGO_DB" --eval "
  const doc = db.getCollection('$MONGO_COLLECTION').findOne(
    { applicationReference: '$APP_REF' },
    { status: 1, 'individuals.personReference': 1, 'individuals.status': 1, failures: 1, 'individuals.failures': 1 }
  );
  if (!doc) { print('NOT_FOUND'); }
  else { print(JSON.stringify(doc, null, 2)); }
")

echo ""
echo "────────────────────────────────────────────────"
echo "  Mongo document for $APP_REF"
echo "────────────────────────────────────────────────"
echo "$RESULT"
echo "────────────────────────────────────────────────"
echo ""

if echo "$RESULT" | grep -q "NOT_FOUND"; then
  fail "Document not found in Mongo for $APP_REF."
fi

APP_STATUS=$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('status','UNKNOWN'))" 2>/dev/null || echo "UNKNOWN")
IND_STATUS=$(echo "$RESULT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('individuals',[{}])[0].get('status','UNKNOWN'))" 2>/dev/null || echo "UNKNOWN")

echo "  Application status : $APP_STATUS"
echo "  Individual status  : $IND_STATUS"
echo ""

if [[ "$APP_STATUS" == "Approved" || "$APP_STATUS" == "SubscribedAndEnrolled" ]]; then
  success "✅  Test PASSED – individual passed risk analysis (application: $APP_STATUS)"
else
  warn "Application status is '$APP_STATUS' (expected Approved or SubscribedAndEnrolled)."
  warn "Check service logs: tail -f logs/agent-registration-risking.log"
  exit 1
fi

