package it.unimore.dipi.iot.smartobject.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.smartobject.message.TelemetryMessage;
import it.unimore.dipi.iot.smartobject.resource.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project mqtt-demo-fleet-monitoring
 * @created 04/11/2020 - 16:01
 */
public class MqttDeviceSmartObject {

    private static final Logger logger = LoggerFactory.getLogger(MqttDeviceSmartObject.class);

    private static final String BASIC_TOPIC = "device";

    private static final String TELEMETRY_TOPIC = "telemetry";

    private static final String EVENT_TOPIC = "event";

    private static final String CONTROL_TOPIC = "control";

    private static final String COMMAND_TOPIC = "command";

    private String deviceId;

    private ObjectMapper mapper;

    private IMqttClient mqttClient;

    private Map<String, SmartObjectResource<?>> resourceMap;

    private boolean singleResourceTelemetryEnabled = false;

    private int targetAggregatedTelemetryPayloadSizeByte = 0;

    public MqttDeviceSmartObject() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Init the vehicle smart object with its ID, the MQTT Client and the Map of managed resources
     * @param vehicleId
     * @param mqttClient
     * @param resourceMap
     */
    public void init(String vehicleId,
                     IMqttClient mqttClient,
                     Map<String, SmartObjectResource<?>> resourceMap,
                     boolean singleResourceTelemetryEnabled){

        this.deviceId = vehicleId;
        this.mqttClient = mqttClient;
        this.resourceMap = resourceMap;
        this.singleResourceTelemetryEnabled = singleResourceTelemetryEnabled;

        logger.info("Vehicle Smart Object correctly created ! Resource Number: {}", resourceMap.keySet().size());
    }

    /**
     * Start vehicle behaviour
     */
    public void start(){

        try{

            if(this.mqttClient != null &&
                this.deviceId != null  && this.deviceId.length() > 0 &&
                this.resourceMap != null && resourceMap.keySet().size() > 0){

                logger.info("Starting Device Emulator ....");

                registerToControlChannel();

                registerToAvailableResources();

            }

        }catch (Exception e){
            logger.error("Error Starting the Vehicle Emulator ! Msg: {}", e.getLocalizedMessage());
        }

    }

    private void registerToControlChannel() {

        try{

            String deviceControlTopic = String.format("%s/%s/%s", BASIC_TOPIC, deviceId, CONTROL_TOPIC);

            logger.info("Registering to Control Topic ({}) ... ", deviceControlTopic);

            this.mqttClient.subscribe(deviceControlTopic, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {

                    if(message != null)
                        logger.info("[CONTROL CHANNEL] -> Control Message Received -> {}", new String(message.getPayload()));
                    else
                        logger.error("[CONTROL CHANNEL] -> Null control message received !");
                }
            });

        }catch (Exception e){
            logger.error("ERROR Registering to Control Channel ! Msg: {}", e.getLocalizedMessage());
        }
    }

    private void registerToAvailableResources(){
        try{

            this.resourceMap.entrySet().forEach(resourceEntry -> {

                if(resourceEntry.getKey() != null && resourceEntry.getValue() != null){
                    SmartObjectResource smartObjectResource = resourceEntry.getValue();

                    //Register to GpsGpxSensorResource Notification
                    if(smartObjectResource.getType().equals(TemperatureRawSensor.RESOURCE_TYPE) && singleResourceTelemetryEnabled){

                        logger.info("Registering to Resource {} (id: {}) notifications ...",
                                smartObjectResource.getType(),
                                smartObjectResource.getId());

                        TemperatureRawSensor temperatureRawSensor = (TemperatureRawSensor)smartObjectResource;
                        temperatureRawSensor.addDataListener(new ResourceDataListener<Double>() {
                            @Override
                            public void onDataChanged(SmartObjectResource<Double> resource, Double updatedValue) {
                                try {
                                    publishTelemetryData(
                                            String.format("%s/%s/%s/%s", BASIC_TOPIC, deviceId, TELEMETRY_TOPIC, resourceEntry.getKey()),
                                            new TelemetryMessage<>(smartObjectResource.getType(), updatedValue));
                                } catch (MqttException | JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    //Register to BatterySensorResource Notification
                    if(smartObjectResource.getType().equals(EnergyRawSensor.RESOURCE_TYPE) && singleResourceTelemetryEnabled){

                        logger.info("Registering to Resource {} (id: {}) notifications ...",
                                smartObjectResource.getType(),
                                smartObjectResource.getId());

                        EnergyRawSensor energyRawSensor = (EnergyRawSensor)smartObjectResource;
                        energyRawSensor.addDataListener(new ResourceDataListener<Double>() {
                            @Override
                            public void onDataChanged(SmartObjectResource<Double> resource, Double updatedValue) {
                                try {
                                    publishTelemetryData(
                                            String.format("%s/%s/%s/%s", BASIC_TOPIC, deviceId, TELEMETRY_TOPIC, resourceEntry.getKey()),
                                            new TelemetryMessage<>(smartObjectResource.getType(), updatedValue));
                                } catch (MqttException | JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                    //Register to AggregatedDeviceStateResource Notification
                    if(smartObjectResource.getType().equals(AggregatedDeviceStateResource.RESOURCE_TYPE)){

                        logger.info("Registering to Resource {} (id: {}) notifications ...",
                                smartObjectResource.getType(),
                                smartObjectResource.getId());

                        AggregatedDeviceStateResource aggregatedDeviceStateResource = (AggregatedDeviceStateResource)smartObjectResource;
                        aggregatedDeviceStateResource.addDataListener(new ResourceDataListener<AggregatedResourcePayload>() {
                            @Override
                            public void onDataChanged(SmartObjectResource<AggregatedResourcePayload> resource,
                                                      AggregatedResourcePayload updatedValue) {
                                try {

                                    //If necessary enrich the payload to reach the target expected payload size
                                    if(targetAggregatedTelemetryPayloadSizeByte > 0){
                                        String defaultPayload = mapper.writeValueAsString(updatedValue);
                                        int payloadSize = defaultPayload.length();
                                        if(targetAggregatedTelemetryPayloadSizeByte - payloadSize > 0)
                                            updatedValue.put("payload_overhead", RandomStringUtils.randomAlphabetic(targetAggregatedTelemetryPayloadSizeByte - payloadSize));
                                    }

                                    publishTelemetryData(
                                            String.format("%s/%s/%s/%s", BASIC_TOPIC, deviceId, TELEMETRY_TOPIC, resourceEntry.getKey()),
                                            new TelemetryMessage<>(smartObjectResource.getType(), updatedValue));
                                } catch (MqttException | JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });

        }catch (Exception e){
            logger.error("Error Registering to Resource ! Msg: {}", e.getLocalizedMessage());
        }
    }

    /**
     * Stop the emulated vehicle
     */
    public void stop(){
        //TODO Implement a proper closing method
    }

    private void publishTelemetryData(String topic, TelemetryMessage<?> telemetryMessage) throws MqttException, JsonProcessingException {

        //logger.info("Sending Telemetry Message to topic: {} -> Data: {}", topic, telemetryMessage);

        if(this.mqttClient != null && this.mqttClient.isConnected() && telemetryMessage != null && topic != null){

            String messagePayload = mapper.writeValueAsString(telemetryMessage);
            publishTelemetryData(topic, messagePayload);
        }
        else
            logger.error("Error: Topic or Msg = Null or MQTT Client is not Connected !");
    }

    private void publishTelemetryData(String topic, String message) throws MqttException, JsonProcessingException {

        logger.info("Sending Message to topic: {} -> Data: {}", topic, message);

        if(this.mqttClient != null && this.mqttClient.isConnected() && message != null && topic != null){

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(0);

//            long leftLimit = 500;
//            long rightLimit = 1500;
//            long generatedLong = leftLimit + (long) (Math.random() * (rightLimit - leftLimit));
//
//            try{
//                Thread.sleep(generatedLong);
//            }catch(Exception e){
//                e.printStackTrace();
//            }

            mqttClient.publish(topic, mqttMessage);

            logger.info("Data Correctly Published to topic: {}", topic);

        }
        else
            logger.error("Error: Topic or Msg = Null or MQTT Client is not Connected !");
    }

    public int getTargetAggregatedTelemetryPayloadSizeByte() {
        return targetAggregatedTelemetryPayloadSizeByte;
    }

    public void setTargetAggregatedTelemetryPayloadSizeByte(int targetAggregatedTelemetryPayloadSizeByte) {
        this.targetAggregatedTelemetryPayloadSizeByte = targetAggregatedTelemetryPayloadSizeByte;
    }
}
