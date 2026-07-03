#!/bin/bash

HTTP_STATUS=$(curl -v \ -w "\n%{http_code}\n" \ -X GET "http://localhost:22203/files-available/list/${INFORMATION_TYPE}")

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "ERROR: Expected HTTP 200 but got $HTTP_STATUS"
  exit 1
fi

echo "OK: HTTP $HTTP_STATUS"
