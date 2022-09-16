#!/bin/bash

docker container stop es01

docker container stop kib01

docker container rm es01

docker container rm kib01

docker network rm esnet

docker volume rm esdata
