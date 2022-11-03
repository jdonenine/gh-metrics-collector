SHELL := /bin/bash

GROUP := jeffdinoto
APP := gh-metrics-collector
VERSION := latest

clean:
	./gradlew clean

build:
	./gradlew build

clean-image:
	docker image rm --force $(GROUP)/$(APP):$(VERSION)

build-image:
	docker build . --tag $(GROUP)/$(APP):$(VERSION)
