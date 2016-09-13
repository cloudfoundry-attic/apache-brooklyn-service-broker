#!/bin/bash -ex

export TERM=dumb

cd brooklyn-broker

./gradlew clean build
mv build/libs/brooklyn-broker.war ../built-brooklyn-broker/brooklyn-broker-$(cat release/tag).war
