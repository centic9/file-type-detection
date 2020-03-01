#!/bin/sh

set -eu

# run file-type-detection on the home directory of the current user
./gradlew  installDist && build/install/file-type-detection/bin/file-type-detection ~ > filetypes.txt
