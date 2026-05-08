#!/usr/bin/env bash

curl -X POST -H "Authorization:1234" -H "content-type:text/json" http://localhost:8470/test-only/token -d @object-store-token.json