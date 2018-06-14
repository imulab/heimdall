#!/bin/bash

destination=$1

mkdir -p ${destination}
find . -type f -regex "${CIRCLE_WORKING_DIRECTORY}/build/test-results/.*xml" -exec cp {} ${destination} \;