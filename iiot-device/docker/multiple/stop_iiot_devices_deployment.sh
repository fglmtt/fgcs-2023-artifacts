#!/bin/bash

echo "Stopping IIoT Device 1 ..."
docker stop iiot-device-1
docker rm iiot-device-1

echo "Stopping IIoT Device 2 ..."
docker stop iiot-device-2
docker rm iiot-device-2

echo "Stopping IIoT Device 3 ..."
docker stop iiot-device-3
docker rm iiot-device-3