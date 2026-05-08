#!/bin/bash

#see application.conf secure-data-exchange-proxy.inbound.information-type
INFORMATION_TYPE="${1:-test-inbound-information-type}"

curl -v \
  -w "\n%{http_code}\n" \
  -X GET "http://localhost:22203/files-available/list/${INFORMATION_TYPE}"


