#!/bin/bash

# Triggers risking by reading uploaded results files directly from MongoDB.
# Use this instead of 50-send-file-ready-notification.sh in QA/Staging where
# the SDES proxy flow cannot be used.
#
# BASE_URL can be overridden to point at a remote environment, e.g.:
#   BASE_URL=https://<staging-host> ./55-trigger-risking-from-mongo.sh
BASE_URL="${BASE_URL:-http://localhost:22203}"

HTTP_STATUS=$(curl -v -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/agent-registration-risking/test-only/trigger-risking")

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "ERROR: Expected HTTP 200 but got $HTTP_STATUS"
  exit 1
fi

echo "OK: HTTP $HTTP_STATUS"

