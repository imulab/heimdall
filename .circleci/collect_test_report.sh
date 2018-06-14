#!/bin/bash

destination=$1
echo "destination is: ${destination}"

mkdir -p ${destination}
find . -type f -regex ".*/build/test-results/.*xml" -exec cp {} ${destination} \;