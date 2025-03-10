#!/bin/bash

# This script starts Docker container with ArangoDB instance.
# The database is populates with a few useful datasets.
# Each next run of the script simply restarts existing instance.
# Usage: $ ./run-database.sh
#
# Read more about ArangoDB Docker installation:
# https://docs.arangodb.com/3.12/operations/installation/docker/

NAME="arangodb"
IMAGE="arangodb:3.12"

CONTAINER_ID=$(docker ps -aq -f name="$NAME")

if [ -n "$CONTAINER_ID" ]; then
  echo "Container '$NAME' exists. Restarting..."
  docker restart "$NAME"
else
  echo "No container named '$NAME' found. Creating one..."
  docker run --env ARANGO_NO_AUTH=1 --publish 8529:8529 --detach --name "$NAME" "$IMAGE"
  pip install --requirement requirements.txt --quiet
  python populate-db.py
fi
