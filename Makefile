SHELL := /bin/bash

CURRENT_IMAGE := $(shell ./gradlew -q printCurrentImage)
CURRENT_GROUP := $(shell ./gradlew -q printGroup)
CURRENT_APP := $(shell ./gradlew -q printName)

clean:
	./gradlew clean

build:
	./gradlew build

build-image: clean build
	./gradlew dockerBuildImage
	docker image tag $(CURRENT_IMAGE) $(CURRENT_GROUP)/$(CURRENT_APP):latest
	docker image ls | grep "$(CURRENT_GROUP)/$(CURRENT_APP)"
