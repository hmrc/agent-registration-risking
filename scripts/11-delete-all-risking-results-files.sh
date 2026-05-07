#!/bin/bash

curl -v \
  -w "\n%{http_code}\n" \
  -X POST "http://localhost:22203/agent-registration-risking/test-only/delete-all-risking-results-files"

