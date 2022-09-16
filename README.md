# metrics-collector

Collect data from Github relevant to the pull request process.

## Overview

## Usage

### Requirements

## Architecture

## Development

In order to develop within the project an accessible ElasticSearch (v7.x) instance must be available.  To make local
development easier, a series of scripts are provided which will start one within Docker.

Before executing these scripts Docker must be running locally.

Executing the following will pull the necessary container images and establish the necessary networks and volumes within
 Docker:

```shell
$ scripts/elastic-setup.sh
```

The following will create, configure, and start a single instance of ElasticSearch within Docker:

```shell
$ scripts/elastic-start.sh
```

As a helper for searching and visualizing the data that is accumulated it can be helpful to also start an instance
of Kibana and connect it to the ElasticSearch instance.

The following will create, configure, and start a single instance of Kibana and attach it to the ElasticSearch instance:

```shell
$ scripts/kibana-start.sh
```

Each of these scripts are intended to be idempotent and can be re-executed safely to either create or also restart 
stopped instances of the containers.

After running these scripts the following containers will be running:

```shell
$ docker container ls
```

```shell
CONTAINER ID   IMAGE                                                  COMMAND                  CREATED          STATUS          PORTS                                                NAMES
0b59c028a920   docker.elastic.co/kibana/kibana:7.17.5                 "/bin/tini -- /usr/l…"   5 seconds ago    Up 4 seconds    127.0.0.1:5601->5601/tcp                             kib01
5fd1e9d93f2b   docker.elastic.co/elasticsearch/elasticsearch:7.17.5   "/bin/tini -- /usr/l…"   37 seconds ago   Up 36 seconds   127.0.0.1:9200->9200/tcp, 127.0.0.1:9300->9300/tcp   es01
```

The applications can now be accessed as follows:

ElasticSearch at [http://127.0.0.1:9200](http://127.0.0.1:9200)

Kibana at [http://127.0.0.1:5601](http://127.0.0.1:5601)

### IntelliJ

An example run configuration is provided to get started quickly when using IntelliJ as your local IDE:

Edit the provided `MetricsCollectorApplication (localhost)` run configuration to provide an appropriate GitHub API 
token, GitHub organization name, and GitHub repository name.  The remainder of the settings are defaulted to match the 
ElasticSearch deployment provided via the local development scripts.
