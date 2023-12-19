#!/bin/bash

DIGITAL_TWIN_ID=$1

docker stop $DIGITAL_TWIN_ID
docker rm $DIGITAL_TWIN_ID