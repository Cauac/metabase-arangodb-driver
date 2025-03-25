#!/bin/bash

# This script builds the driver and then deploys it to the local Metabase instance.
# Usage: $ ./run-dev-container.sh
#

# STEP 1: build the driver
cd ..
lein uberjar

# STEP 2: start Docker container with Metabase instance.

NAME="metarango"
IMAGE="metabase/metabase:v0.52.14"

deploy_driver() {
  echo "Deploying the driver jar"
  docker cp ./target/uberjar/arangodb.metabase-driver.jar "$NAME":/plugins/arangodb.metabase-driver.jar
}

CONTAINER_ID=$(docker ps -aq -f name="$NAME")

if [ -n "$CONTAINER_ID" ]; then
  echo "Container '$NAME' exists. Restarting..."
  deploy_driver
  docker restart "$NAME"
else
  echo "No container named '$NAME' found. Creating one..."
  docker run --publish 3000:3000 --detach --name "$NAME" "$IMAGE"
  deploy_driver
fi
