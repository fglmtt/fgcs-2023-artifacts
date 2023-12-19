#!/bin/bash

docker run --name=iiot-device-1 \
    -p 5555:5555 \
    -v $(pwd)/device_conf.yaml:/usr/local/src/mvnapp/device_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>