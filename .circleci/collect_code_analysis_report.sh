#!/bin/bash

destination=$1

mkdir -p ${destination}
find . -type f -regex ".*/reports/.*xml" -exec cp {} ${destination} \;
find . -type f -regex ".*/reports/.*html" -exec cp {} ${destination} \;