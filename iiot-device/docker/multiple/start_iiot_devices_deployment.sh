#!/bin/bash

echo "Starting IIoT Device 1 ..."
docker run --name=iiot-device-1 \
    -p 6555:5555 \
    -v $(pwd)/device_conf_1.yaml:/usr/local/src/mvnapp/device_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>

echo "Starting IIoT Device 2 ..."
docker run --name=iiot-device-2 \
    -p 7555:5555 \
    -v $(pwd)/device_conf_2.yaml:/usr/local/src/mvnapp/device_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>

echo "Starting IIoT Device 3 ..."
docker run --name=iiot-device-3 \
    -p 8555:5555 \
    -v $(pwd)/device_conf_3.yaml:/usr/local/src/mvnapp/device_conf.yaml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/src/main/resources/logback.xml \
    -v $(pwd)/logback.xml:/usr/local/src/mvnapp/target/classes/logback.xml \
    -d <YOUR_CONTAINER_ID>:<YOUR_VERSION>