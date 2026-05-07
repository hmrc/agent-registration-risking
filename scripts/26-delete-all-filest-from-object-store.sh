#!/bin/bash

#bug ...
find /tmp/object-store/agent-registration-risking/*/ -type f | while read -r file; do
  echo "Deleting: $file"
  rm "$file"
done
