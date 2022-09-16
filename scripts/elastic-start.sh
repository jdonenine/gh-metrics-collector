#!/bin/bash

docker container inspect es01 || docker run -d --name es01 --net esnet -p 127.0.0.1:9200:9200 -p 127.0.0.1:9300:9300 -v esdata:/usr/share/elasticsearch/data -e "discovery.type=single-node" -v "$(pwd)"/elastic-password.txt:/run/secrets/bootstrapPassword.txt -e ELASTIC_PASSWORD_FILE=/run/secrets/bootstrapPassword.txt docker.elastic.co/elasticsearch/elasticsearch:7.17.5

docker container start es01
