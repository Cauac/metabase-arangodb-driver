## Development

### Prerequisites

* Java >=8
* [Leiningen](https://leiningen.org/)
* Docker
* Maven
* Python (optional)

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

### Install Metabase dependency
The driver source code references Metabase namespaces and interfaces.
In order to build the driver Metabase needs to be installed as project dependency.
It can be done with the script:
```shell
./install-deps.sh 0.52.14
```

### Run local Metabase instance with the driver

Run the following command to re-build the driver and deploy 
it to the local Metabase instance running in Docker container.

```shell
./run-dev-container.sh
```
By default, the Metabase instance is available at http://localhost:3000.
