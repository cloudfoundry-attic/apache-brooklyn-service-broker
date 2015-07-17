#!/bin/bash

cd broker-parent

./gradlew publishToMavenLocal

cd ../brooklyn-broker

./gradlew check
./gradlew clean build

