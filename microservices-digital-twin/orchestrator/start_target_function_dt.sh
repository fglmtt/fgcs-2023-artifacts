#!/bin/bash

DIGITAL_TWIN_ID=$1
DT_VERSION=$2

docker run --name=$DIGITAL_TWIN_ID \
    -v $(pwd)/$DIGITAL_TWIN_ID.yaml:/usr/local/src/mvnapp/dt_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d registry.gitlab.com/fluid-digital-twins/dt-fluid-function:$DT_VERSION