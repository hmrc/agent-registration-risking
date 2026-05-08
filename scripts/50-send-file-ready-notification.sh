#!/bin/bash

HTTP_STATUS=$(curl -v -o /dev/null -w "%{http_code}" -X POST http://localhost:22203/agent-registration-risking/receive-sdes-notifications \
  -H "Content-Type: application/json" \
  -d @"$(dirname "$0")/notification-file-ready.json")

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "ERROR: Expected HTTP 200 but got $HTTP_STATUS"
  exit 1
fi

echo "OK: HTTP $HTTP_STATUS"
