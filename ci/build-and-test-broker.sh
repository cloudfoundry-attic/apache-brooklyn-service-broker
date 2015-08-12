#!/bin/bash -ex

export TERM=dumb

cd broker-parent

./gradlew publishToMavenLocal

cd ../brooklyn-broker

./gradlew clean build

mv build/libs/brooklyn-broker.war build/libs/brooklyn-broker-$(cat release/tag).war
