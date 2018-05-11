#!/bin/sh

## Start application in dev mode with remote debugging
sbt compile && sbt -jvm-debug 9999 \
    -Dlogback.configurationFile=conf/logback-dev.xml \
    run
