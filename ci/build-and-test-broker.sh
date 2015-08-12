#!/bin/bash -ex

export TERM=dumb

cd broker-parent

./gradlew publishToMavenLocal

cd ../brooklyn-broker

./gradlew clean build

cp build/libs/brooklyn-broker.war /tmp
ls /tmp
