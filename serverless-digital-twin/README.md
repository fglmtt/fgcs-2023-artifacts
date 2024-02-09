# Serverless Digital Twins with Fission

## Requirements

What you need:
* Docker installed on your development machine
* A Kubernetes cluster up and running (see [here](../kubemake/README.md))
* Helm (see [here](https://helm.sh/docs/intro/install/))

## Install Fission

Create a namespace:
```
export FISSION_NAMESPACE="fission"
kubectl create namespace $FISSION_NAMESPACE
```

Create custom resource definitions:
```
kubectl create -k "github.com/fission/fission/crds/v1?ref=v1.20.0"
```

Deploy Fission through Helm:
```
helm repo add fission-charts https://fission.github.io/fission-charts/
helm repo update
helm install \
    --version v1.20.0 \
    --namespace $FISSION_NAMESPACE \
    fission \
    --set serviceType=NodePort \
    --set routerServiceType=NodePort \
    --set persistence.enabled=false \
    fission-charts/fission-all
```

**Note 1**: In contrast to the [official installation](https://fission.io/docs/installation/), this adds `persistence.enabled=false`.

**Note 2**: Pay attention if there is Istio sidecar injection enabled in the namespace where you will deploy the functions, either modify the Helm [values](https://artifacthub.io/packages/helm/fission-charts/fission-all) accordingly or disable it.

## Environment Setup

Create the environment:
```
docker build \
    -t <username>/python-postgres:latest \
    --build-arg PY_BASE_IMG=3.7-alpine \
    -f env/Dockerfile \
    env
docker push <username>/python-postgres:latest
```

Deploy it:
```
fission env create \
    --name postgres \
    --image <username>/python-postgres \
    --builder fission/python-builder:latest
```

Also deploy the plain Python environment:
```
fission env create \
    --name python \
    --image fission/python-env
```

Deploy Postgres:

`postgres.yml`
```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
spec:
  selector:
    matchLabels:
      app: postgres
  replicas: 1
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:latest
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: <database>
        - name: POSTGRES_USER
          value: <user>
        - name: POSTGRES_PASSWORD
          value: <password>
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
spec:
  selector:
    app: postgres
  ports:
    - protocol: TCP
      port: 5432
      targetPort: 5432
  type: NodePort
```

```
kubectl apply -f postgres.yml
```

Create the following tables:
```sql
CREATE TABLE physicalstate (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(255),
    device_timestamp BIGINT,
    timestamp BIGINT,
    temperature REAL,
    energy REAL
);

CREATE TABLE digitalstate (
    id SERIAL PRIMARY KEY,
    physicalstate_id INTEGER REFERENCES physicalstate(id),
    device_id VARCHAR(255),
    timestamp BIGINT,
    temperature REAL,
    energy REAL,
    availability REAL,
    reliability REAL,
    timeliness REAL,
    odte REAL,
    lifecycle VARCHAR(255)
);
```

Deploy an MQTT broker:

`physical-broker.yml`
```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: physical-broker
data:
  mosquitto.conf: |
    allow_anonymous true
    listener 1883
    persistence true
    persistence_location /mosquitto/data/
    log_type all
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: physical-broker
  labels:
    app: physical-broker
spec:
  replicas: 1
  selector:
    matchLabels:
      app: physical-broker
  template:
    metadata:
      labels:
        app: physical-broker
    spec:
      containers:
      - name: physical-broker
        image: eclipse-mosquitto
        ports:
        - containerPort: 1883
        - containerPort: 9001
        volumeMounts:
        - name: data
          mountPath: /mosquitto/config/mosquitto.conf
          subPath: mosquitto.conf
      initContainers:
      - name: copy
        image: eclipse-mosquitto
        command: ["sh", "-c", "cp /tmp/config/mosquitto.conf /tmp/data/"]
        volumeMounts:
        - mountPath: /tmp/config
          name: config
        - mountPath: /tmp/data
          name: data
      volumes:
      - name: data
        emptyDir: {}
      - name: config
        configMap: 
          name: physical-broker
---
apiVersion: v1
kind: Service
metadata:
  name: physical-broker
spec:
  type: NodePort
  selector:
    app: physical-broker
  ports:
  - name: tcp-default
    protocol: TCP
    port: 1883
    targetPort: 1883
    nodePort: 31883
  - name: tcp-websocket
    protocol: TCP
    port: 9001
    targetPort: 9001
```

```
kubectl apply -f physical-broker.yml
```

Deploy an IIoT device that publishes physical events ([here](../iiot-device/README.md) the instructions to build the container image):

`iiot-device-1.yml`
```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: iiot-device-1
data:
  device_conf.yaml: |
    deviceId: iiot-device-1
    targetMqttBrokerAddress: physical-broker.default.svc.cluster.local
    targetMqttBrokerPort: 1883
    httpApiPort: 5555
    updatePeriodMs: 5000
    updateInitialDelayMs: 2000
    aggregatedTelemetry: true
    aggregatedTelemetryMsgSec: 0.2
    singleResourceTelemetryEnabled: false
    resourceMap:
      energy: iot.sensor.energy
      temperature: iot.sensor.temperature
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: iiot-device-1
  labels:
    app: iiot-device-1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: iiot-device-1
  template:
    metadata:
      labels:
        app: iiot-device-1
    spec:
      containers:
      - name: iiot-device-1
        image: <username>/iiot-device
        ports:
        - name: http-api
          protocol: TCP
          containerPort: 5555
        volumeMounts:
        - name: config
          mountPath: /usr/local/src/mvnapp/device_conf.yaml
          subPath: device_conf.yaml
      volumes:
      - name: config
        configMap: 
          name: iiot-device-1
---
apiVersion: v1
kind: Service
metadata:
  name: iiot-device-1
spec:
  type: NodePort
  selector:
    app: iiot-device-1
  ports:
  - name: http-api
    protocol: TCP
    port: 5555
    targetPort: 5555
```

It is also necessary to create the ConfigMaps from which the functions will get config values.

`postgres-cm.yml`
```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres
data:
  database: <database>
  user: <user>
  password: <password>
  host: "postgres"
  port: "5432"
```

`odte-cm.yml`
```yaml
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: odte
data:
  expected_update_rate: "0.2"
  time_window: "30"
  desired_timeliness: "1"
  threshold: "0.9"
```

```
kubectl apply -f postgres-cm.yml
kubectl apply -f odte-cm.yml
```

## Source Packages Deployment

Package source code:
```
./package.sh
```

Create source packages:
```
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
```

## Function Creation

Create functions:
```
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
```

## Route Creation

Create routes:
```
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
```

## Function Invocation

Build the `mqtt-connector` (this is the component that invokes the functions):

```
docker build \
    -t <username>/mqtt-connector \
    -f mqtt-connector/Dockerfile \
    mqtt-connector
```

Deploy it:

`mqtt-connector.yml`
```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mqtt-connector
  labels:
    app: mqtt-connector
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mqtt-connector
  template:
    metadata:
      labels:
        app: mqtt-connector
    spec:
      containers:
      - name: mqtt-connector
        image: <username>/mqtt-connector:latest
        imagePullPolicy: Always
        command: ["python3", "app.py"]
        args: [
          "--broker-url", "physical-broker:1883",
          "--topic", "device/iiot-device-1/telemetry/device_state",
          "--router-url", "router.fission"
        ]
```

```
kubectl apply -f mqtt-connector.yml
```
