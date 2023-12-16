#!/bin/sh

# package source code
./package.sh

# create packages
fission pkg create \
    --name physical-event-handler-pkg \
    --sourcearchive physical-event-handler.zip \
    --env postgres \
    --buildcmd "./build.sh"

fission pkg create \
    --name shadow-handler-pkg \
    --sourcearchive shadow-handler.zip \
    --env postgres \
    --buildcmd "./build.sh"

fission pkg create \
    --name twin-handler-pkg \
    --sourcearchive twin-handler.zip \
    --env postgres \
    --buildcmd "./build.sh"

# create functions
fission fn create \
    --name physical-event-handler \
    --pkg physical-event-handler-pkg \
    --entrypoint "func.main" \
    --configmap postgres

fission fn create \
    --name shadow-handler \
    --pkg shadow-handler-pkg \
    --entrypoint "func.main" \
    --configmap postgres

fission fn create \
    --name twin-handler \
    --pkg twin-handler-pkg \
    --entrypoint "func.main" \
    --configmap postgres \
    --configmap odte

fission fn create \
    --name digital-event-handler \
    --env python \
    --code digital-event-handler/func.py

# create routes
fission route create \
    --name physical-event-handler \
    --method POST \
    --url /physicalevents \
    --function physical-event-handler

fission route create \
    --name shadow-handler \
    --method POST \
    --url /physicalstates \
    --function shadow-handler

fission route create \
    --name twin-handler \
    --method POST \
    --url /pendingdigitalstates \
    --function twin-handler

fission route create \
    --name digital-event-handler \
    --method POST \
    --url /digitalstates \
    --function digital-event-handler