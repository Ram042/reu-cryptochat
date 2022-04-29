#!/bin/bash

./gradlew :cli:run --debug-jvm --q --console=plain --args="$*"
