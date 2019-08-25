# metabase-arangodb-driver

Allows Metabase to connect to ArangoDB.

## Build

Build command: `lein uberjar`

Result file location: `/targer/uberjar/arangodb.metabase-driver.jar`

## Installation

1. build jar
2. put driver jar file into metabase plugin folder
3. restart metabase

## Run locally

```bash
lein uberjar

docker run -d -p 3000:3000 --name metabase metabase/metabase

# find created container id with 'docker ps' command

docker cp <full path to driver file> <metabase-container-id>:/plugins/arangodb.metabase-driver.jar

docker restart <metabase-container-id>

```
