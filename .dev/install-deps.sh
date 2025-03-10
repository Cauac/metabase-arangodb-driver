#!/bin/bash

# This script downloads a specified version of the Metabase jar file and installs it as a Maven artifact.

# Check if version parameter is provided
if [ -z "$1" ]; then
  echo "Usage: $0 <metabase-version>"
  echo "Example: $0 0.52.14"
  exit 1
fi

METABASE_VERSION="$1"
TMP_DIR=$(mktemp -d)

# Download the Metabase jar file to a temporary directory
curl --location "https://downloads.metabase.com/v${METABASE_VERSION}.x/metabase.jar" --output "${TMP_DIR}/metabase.jar"

# Install the downloaded jar as a Maven artifact
mvn install:install-file \
  -Dfile="${TMP_DIR}/metabase.jar" \
  -DartifactId=metabase-core \
  -Dversion="${METABASE_VERSION}" \
  -DgroupId=metabase \
  -Dpackaging=jar

# Cleanup temporary directory
rm -rf "${TMP_DIR}"