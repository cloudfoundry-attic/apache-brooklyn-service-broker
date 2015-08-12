#!/bin/bash -ex

export TERM=dumb

cd broker-parent

./gradlew publishToMavenLocal

cd ../brooklyn-broker

./gradlew check
./gradlew clean build
