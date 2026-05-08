#!/bin/bash

RISKING_RESULTS_FILE_NAME="${1:-}"

if [ -z "${RISKING_RESULTS_FILE_NAME}" ]; then
  echo "Error: No file name provided. Please provide the risking results file name as an argument."
  exit 1
fi

if [ ! -f "${RISKING_RESULTS_FILE_NAME}" ]; then
  echo "Error: File '${RISKING_RESULTS_FILE_NAME}' does not exist."
  exit 1
fi

curl -v \
  -o /dev/null \
  -w "%{http_code}" \
  -X POST http://localhost:22203/agent-registration-risking/test-only/risking-results-file/${RISKING_RESULTS_FILE_NAME} \
  -H "Content-Type: application/json" \
  -d @"${RISKING_RESULTS_FILE_NAME}"
