#!/usr/bin/env bash
# Adapted from github.com/ajalt/clikt



./gradlew --quiet :app:installDist

./app/build/install/app/bin/app "$@"
