## Development

### Prerequisites

* Java
* [Leiningen](https://leiningen.org/)
* Docker
* Maven
* Python (optional)

### Install Metabase dependency
The driver source code references Metabase namespaces and interfaces.
To be able to build the driver Metabase needs to be installed as project dependency.
It can be done with the script:
```shell
./install-deps.sh 0.52.14
```

### Build the driver
In order to build the driver run the following command from the project root directory.

```shell
lein uberjar
```
When build process is over, the driver jar file can be found in `/targer/uberjar/arangodb.metabase-driver.jar`

### Run local ArangoDB instance

The following script will create ArangoDB and initiate it with data.
```shell
./run-database.sh
```
Default configuration:
* **url:** http://localhost:8529
* **database:** test_db
* **user:** metabase
* **password:** 1111

### Run local Metabase instance with the driver

Run the following command to re-build the driver and deploy 
it to the local Metabase instance running in Docker container.

```shell
./run-dev-container.sh
```
By default, the Metabase instance is available at http://localhost:3000.
