#!/bin/bash

docker pull docker.elastic.co/elasticsearch/elasticsearch:7.17.5

docker pull docker.elastic.co/kibana/kibana:7.17.5

docker network inspect esnet || docker network create esnet

docker volume inspect esdata || docker volume create esdata
