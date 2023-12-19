package it.unimore.dipi.iot.smartobject.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;
import it.unimore.dipi.iot.smartobject.device.MqttDeviceSmartObject;
import it.unimore.dipi.iot.smartobject.resource.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project mqtt-demo-fleet-monitoring
 * @created 04/11/2020 - 16:15
 */
public class DeviceProcess {

    private static final Logger logger = LoggerFactory.getLogger(DeviceProcess.class);

    private static final String DEVICE_CONFIGURATION_FILE_PATH = "device_conf.yaml";

    private static Map<String, SmartObjectResource<?>> resourceMap;

    private static DeviceConfiguration deviceConfiguration;

    private static HttpConfigurationHandler httpConfigurationHandler;

    public static void main(String[] args) {

        try{

            deviceConfiguration = readConfigurationFile();
            buildResourceMap(deviceConfiguration.getResourceMap());

            if(deviceConfiguration.getAggregatedTelemetry() == true)
                buildAggregatedResource();

            //Create HTTP Configuration Handler
            httpConfigurationHandler = new HttpConfigurationHandler(deviceConfiguration, resourceMap);

            //Start HTTP API to handle device configuration
            startDeviceHttpApi();

            if(deviceConfiguration != null){

                //Create MQTT Client
                MqttClientPersistence persistence = new MemoryPersistence();
                IMqttClient mqttClient = new MqttClient(String.format("tcp://%s:%d",
                        deviceConfiguration.getTargetMqttBrokerAddress(),
                        deviceConfiguration.getTargetMqttBrokerPort()),
                        deviceConfiguration.getDeviceId(),
                        persistence);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setConnectionTimeout(10);

                //Connect to MQTT Broker
                mqttClient.connect(options);

                logger.info("MQTT Client Connected ! Client Id: {}", deviceConfiguration.getDeviceId());

                MqttDeviceSmartObject mqttDeviceSmartObject = new MqttDeviceSmartObject();
                mqttDeviceSmartObject.init(deviceConfiguration.getDeviceId(),
                        mqttClient,
                        resourceMap,
                        deviceConfiguration.getSingleResourceTelemetryEnabled());

                //Configure the target expected payload size
                mqttDeviceSmartObject.setTargetAggregatedTelemetryPayloadSizeByte(deviceConfiguration.getTargetAggregatedTelemetryPayloadSizeByte());

                mqttDeviceSmartObject.start();
            }
            else
                logger.error("Error Loading DeviceConfiguration from file: {}", DEVICE_CONFIGURATION_FILE_PATH);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void buildResourceMap(Map<String, String> targetResourceStringMap){

        if(resourceMap == null)
            resourceMap = new HashMap<>();

        for (Map.Entry<String, String> entry : targetResourceStringMap.entrySet()) {

            String resourceKey = entry.getKey();
            String resourceType = entry.getValue();

            if(resourceType.equals(EnergyRawSensor.RESOURCE_TYPE))
                resourceMap.put(resourceKey,
                        new EnergyRawSensor(
                                deviceConfiguration.getUpdatePeriodMs(),
                                deviceConfiguration.getUpdateInitialDelayMs()));
            else if(resourceType.equals(TemperatureRawSensor.RESOURCE_TYPE))
                resourceMap.put(resourceKey,
                        new TemperatureRawSensor(
                                deviceConfiguration.getUpdatePeriodMs(),
                                deviceConfiguration.getUpdateInitialDelayMs()));
            else
                logger.error("ERROR ! Resource Type: {} NOT FOUND for Resource Key: {}", resourceType, resourceKey);

        }
    }

    private static void buildAggregatedResource(){

        List<SmartObjectResource<?>> resourceList = new ArrayList<>();

        resourceMap.entrySet().forEach(resourceEntry -> {
            if(resourceEntry != null && resourceEntry.getValue() != null)
                resourceList.add(resourceEntry.getValue());
        });

        if(resourceList.size() > 0){
            AggregatedDeviceStateResource aggregatedDeviceStateResource = new AggregatedDeviceStateResource(
                    deviceConfiguration.getAggregatedTelemetryMsgSec(),
                    deviceConfiguration.getUpdateInitialDelayMs(),
                    resourceList);

            resourceMap.put("device_state", aggregatedDeviceStateResource);
        }
    }

    private static void startDeviceHttpApi(){
        try{

            Undertow server = Undertow.builder().addHttpListener(
                    deviceConfiguration.getHttpApiPort(),
                    "0.0.0.0",
                    getBasicRoutes()).build();

            server.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static final HttpHandler getBasicRoutes() {
        return new RoutingHandler()
                .add(Methods.GET, "/conf", httpConfigurationHandler)
                .add(Methods.PUT, "/conf", httpConfigurationHandler);
    }

    private static DeviceConfiguration readConfigurationFile() {
        try{
            //ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //File file = new File(classLoader.getResource(WLDT_CONFIGURATION_FILE).getFile());
            File file = new File(DEVICE_CONFIGURATION_FILE_PATH);
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            return om.readValue(file, DeviceConfiguration.class);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
