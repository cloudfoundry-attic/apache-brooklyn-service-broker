#!/bin/bash -ex

export TERM=dumb

cd brooklyn-broker

./gradlew clean build

mv build/libs/brooklyn-service-broker.war build/libs/brooklyn-broker-$(cat release/tag).war
