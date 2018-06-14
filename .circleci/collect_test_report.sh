#!/bin/bash

destination=$1

mkdir -p ${destination}
find . -type f -regex ".*/build/test-results/.*xml" -exec cp {} ${destination} \;