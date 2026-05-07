#!/usr/bin/env zsh
# =============================================================================
# local-risking-test.sh
#
# Manual local processing helper:
#   1. Assumes you have pre-created a risking record in your local Mongo
#   2. You edit Step 4 below with the result file content you want to test
#   3. Script creates the results file, serves it via temporary HTTP server
#   4. Patches SDES stub canned response to point at the file
#   5. Triggers the skip-upload endpoint to process the file
#
# Prerequisites (all must be running before you execute this script):
#   - agent-registration-risking:
#       cd agent-registration-risking
#       sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run
#   - secure-data-exchange-list-files-stubs:
#       cd secure-data-exchange-list-files-stubs
#       sbt -Dhttp.port=8765 run
#   - MongoDB on localhost:27017 with pre-created risking record
#   - python3, curl
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
RESULTS_FILE_PORT=19999
RUN_ID="$(date +%Y%m%d_%H%M%S)_$$"
RESULTS_FILE_NAME="risking_results_${RUN_ID}.json"
RESULTS_FILE_PATH="/tmp/${RESULTS_FILE_NAME}"
RESULTS_FILE_URL="http://localhost:${RESULTS_FILE_PORT}/${RESULTS_FILE_NAME}"

# Override this per machine if needed:
#   export SDES_STUB_CANNED_RESPONSE="/path/to/secure-data-exchange-list-files-stubs/conf/responses/downloads/apiDownloadFiles.json"
SDES_STUB_CANNED_RESPONSE="${SDES_STUB_CANNED_RESPONSE:-$HOME/workspace/secure-data-exchange-list-files-stubs/conf/responses/downloads/apiDownloadFiles.json}"

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
info "Step 0 - Checking prerequisites..."
for cmd in python3 curl; do
  has_cmd "$cmd" || fail "'$cmd' is not installed."
done
success "Tools OK."

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/ping/ping" || true)
[[ "$HTTP_STATUS" == "200" ]] || fail "agent-registration-risking not responding (HTTP $HTTP_STATUS). Start with: sbt -Dapplication.router=testOnlyDoNotUseInAppConf.Routes run"
success "Service is up."

curl -s -o /dev/null -w "%{http_code}" "http://localhost:8765/ping/ping" | grep -q "200" \
  || warn "SDES stub may not be running on :8765 - start with: sbt -Dhttp.port=8765 run (in secure-data-exchange-list-files-stubs)"

# ── Step 1: Confirm manual setup ──────────────────────────────────────────────
info "Step 1 - Manual setup required"
success "You have a risking record pre-created in Mongo (manually done outside this script)."

# ── Step 2: Write results file (EDIT THIS PAYLOAD FOR YOUR SCENARIO) ──────────
info "Step 2 - Writing results file to $RESULTS_FILE_PATH..."
cat > "$RESULTS_FILE_PATH" <<'EOF'
[
  {
    "recordType": "Entity",
    "applicationReference": "XKXP9HEZB",
    "failures": [
      {
        "reasonCode": "7",
        "reasonDescription": "Insolvent",
        "checkId": "7",
        "checkDescription": "Insolvent"
      }
    ]
  },
  {
    "recordType": "Individual",
    "personReference": "JJFCXYTM4",
    "failures": [
      {
        "reasonCode": "9",
        "reasonDescription": "Relevant criminal convictions",
        "checkId": "9",
        "checkDescription": "Relevant criminal convictions"
      }
    ]
  },
  {
    "recordType": "Individual",
    "personReference": "ZHQGTDK8Y",
    "failures": [
      {
        "reasonCode": "6",
        "reasonDescription": "Disqualified as a director on Companies House",
        "checkId": "6",
        "checkDescription": "Disqualified as a director on Companies House"
      },
      {
        "reasonCode": "9",
        "reasonDescription": "Relevant criminal convictions",
        "checkId": "9",
        "checkDescription": "Relevant criminal convictions"
      }
    ]
  }
]
EOF
FILE_SIZE=$(wc -c < "$RESULTS_FILE_PATH" | tr -d ' ')
success "Results file written ($FILE_SIZE bytes)."
info "Edit the JSON above (in Step 2) to match your risking record and desired failures."

# ── Step 3: Serve the results file ───────────────────────────────────────────
info "Step 3 - Starting temporary HTTP file server on port $RESULTS_FILE_PORT..."
pkill -f "http.server $RESULTS_FILE_PORT" 2>/dev/null || true
sleep 0.5
python3 -m http.server "$RESULTS_FILE_PORT" --directory /tmp &>/tmp/local-risking-http-server.log &
HTTP_SERVER_PID=$!
sleep 1
kill -0 "$HTTP_SERVER_PID" 2>/dev/null || fail "Could not start HTTP server on port $RESULTS_FILE_PORT - check it is free."
success "File server running (PID $HTTP_SERVER_PID) -> $RESULTS_FILE_URL"

# ── Step 4: Patch SDES stub canned response ──────────────────────────────────
info "Step 4 - Patching SDES stub canned response -> $SDES_STUB_CANNED_RESPONSE..."
if [[ ! -f "$SDES_STUB_CANNED_RESPONSE" ]]; then
  fail "Canned response file not found: $SDES_STUB_CANNED_RESPONSE"
fi

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
success "Canned response patched."

# Give sbt dev-mode classloader time to reload changes.
info "Waiting 5s for SDES stub to reload canned response..."
sleep 5

# ── Step 5: Trigger processing ───────────────────────────────────────────────
info "Step 5 - Triggering download-available-results-files-skip-upload..."
TRIGGER_RESPONSE=$(curl -s -w "\n%{http_code}" \
  "$BASE_URL/agent-registration-risking/test-only/download-available-results-files-skip-upload")
TRIGGER_BODY=$(echo "$TRIGGER_RESPONSE" | sed '$d')
TRIGGER_STATUS=$(echo "$TRIGGER_RESPONSE" | tail -n 1)

if [[ "$TRIGGER_STATUS" == "200" ]]; then
  success "Endpoint returned HTTP 200. Response: $TRIGGER_BODY"
else
  fail "Endpoint returned HTTP $TRIGGER_STATUS. Body: $TRIGGER_BODY"
fi

info "Done. Verify Mongo record manually based on your expected scenario."

