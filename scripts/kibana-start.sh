#!/bin/bash

docker container inspect kib01 || docker run -d --name kib01 --net esnet -p 127.0.0.1:5601:5601 -e "ELASTICSEARCH_HOSTS=http://es01:9200" docker.elastic.co/kibana/kibana:7.17.5

docker container start kib01
