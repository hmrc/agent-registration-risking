#!/bin/bash

HTTP_STATUS=$(curl -s \
  -o /dev/null \
  -w "%{http_code}" \
  -X GET "http://localhost:22203/agent-registration-risking/test-only/run-results-file-processing")

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "ERROR: Expected HTTP 200 but got $HTTP_STATUS"
  exit 1
fi

echo "OK: HTTP $HTTP_STATUS"
