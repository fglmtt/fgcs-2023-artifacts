# IIoT Device Emulator

This project implements a simple emulator for IIoT MQTT device handling the following resources and JSON messages: 

Telemetry Energy Topic:

```
device/testDevice1/telemetry/energy
```

The associated payload is:

```json
{"timestamp":1646847376104,"type":"iot.sensor.energy","data":4.669321957257875}
```

Telemetry Temperature Topic:

```
device/testDevice1/telemetry/temperature
```

The associated payload is:

```json
{"timestamp":1646847376104,"type":"iot.sensor.temperature","data":24.28418525817319}
```

It is configurable through the file ```device_conf.yaml``` in order to specify:

- deviceId: Device Id
- httpApiPort: HTTP API Listening port
- targetMqttBrokerAddress: Target MQTT Broker Address
- targetMqttBrokerPort: Target MQTT Broker Port
- updatePeriodMs: Update Period (ms)
- updateInitialDelayMs: Initial Delay before start sending update (ms)
- resourceMap: a Map of Key-String to specify the list of managed resource/sensors/actuators. 
The first value represent the internal key of the resource (used to build target topic)
while the second one identify the resource type.

An example of configuration file is the following:

```yaml
deviceId: testDevice1
httpApiPort: 5555
targetMqttBrokerAddress: 127.0.0.1
targetMqttBrokerPort: 1883
updatePeriodMs: 1000
updateInitialDelayMs: 2000
aggregatedTelemetry: true
aggregatedTelemetryMsgSec: 1
singleResourceTelemetryEnabled: false
targetAggregatedTelemetryPayloadSizeByte: 1
resourceMap:
  energy: iot.sensor.energy
  temperature: iot.sensor.temperature
```

## HTTP API - Configuration Management

A dedicated HTTP API has been added to read the current device configuration and 
control parameters related to sensor update period (ms) and update initial delay.

Configuration Resource URL: http://<ip_address>:<server_port>/conf

### READ The Current Configuration

HTTP METHOD: GET
URL: http://<ip_address>:<server_port>/conf
REQUEST BODY: Empty
RESPONSE CODE: 200 OK
RESPONSE BODY:

```json
{
    "deviceId": "testDevice1",
    "targetMqttBrokerAddress": "127.0.0.1",
    "targetMqttBrokerPort": 1883,
    "updatePeriodMs": 1000,
    "updateInitialDelayMs": 2000,
    "resourceMap": {
        "energy": "iot.sensor.energy",
        "temperature": "iot.sensor.temperature"
    },
    "httpApiPort": 5555
}
```

### UPDATE The Current Configuration (Option 1 - Full Conf)

HTTP METHOD: PUT
URL: http://<ip_address>:<server_port>/conf
REQUEST BODY: 
```json
{
  "deviceId": "testDevice1",
  "targetMqttBrokerAddress": "127.0.0.1",
  "targetMqttBrokerPort": 1883,
  "updatePeriodMs": 1000,
  "updateInitialDelayMs": 2000,
  "resourceMap": {
    "energy": "iot.sensor.energy",
    "temperature": "iot.sensor.temperature"
  },
  "httpApiPort": 5555,
  "aggregatedTelemetry": true,
  "aggregatedTelemetryMsgSec": 0.5,
  "singleResourceTelemetryEnabled": false,
  "targetAggregatedTelemetryPayloadSizeByte": 1
}
```
RESPONSE CODE: 200 OK
RESPONSE BODY: Empty

### UPDATE The Current Configuration (Option 2 - Only Time Values Conf)

HTTP METHOD: PUT
URL: http://<ip_address>:<server_port>/conf
REQUEST BODY:
```json
{
  "updatePeriodMs": 5000,
  "updateInitialDelayMs": 2000,
  "aggregatedTelemetryMsgSec": 0.5
}
```
RESPONSE CODE: 200 OK
RESPONSE BODY: Empty


