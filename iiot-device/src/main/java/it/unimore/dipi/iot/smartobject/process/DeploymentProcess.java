package it.unimore.dipi.iot.smartobject.process;

import it.unimore.dipi.iot.smartobject.device.MqttDeviceSmartObject;
import it.unimore.dipi.iot.smartobject.resource.EnergyRawSensor;
import it.unimore.dipi.iot.smartobject.resource.SmartObjectResource;
import it.unimore.dipi.iot.smartobject.resource.TemperatureRawSensor;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Random;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project mqtt-demo-fleet-monitoring
 * @created 04/11/2020 - 16:15
 */
public class DeploymentProcess {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentProcess.class);

    private static String MQTT_BROKER_IP = "127.0.0.1";

    private static int MQTT_BROKER_PORT = 1883;

    public static void main(String[] args) {

        try{

            executeNewDeviceEmulator("testDevice1");

            Thread.sleep(5000);
            executeNewDeviceEmulator("testDevice2");

            Thread.sleep(5000);
            executeNewDeviceEmulator("testDevice3");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void executeNewDeviceEmulator(String deviceId){

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try{

                        //Create MQTT Client
                        MqttClientPersistence persistence = new MemoryPersistence();
                        IMqttClient mqttClient = new MqttClient(String.format("tcp://%s:%d",
                                MQTT_BROKER_IP,
                                MQTT_BROKER_PORT),
                                deviceId,
                                persistence);

                        MqttConnectOptions options = new MqttConnectOptions();
                        options.setAutomaticReconnect(true);
                        options.setCleanSession(true);
                        options.setConnectionTimeout(10);

                        //Connect to MQTT Broker
                        mqttClient.connect(options);

                        logger.info("MQTT Client Connected ! Client Id: {}", deviceId);

                        MqttDeviceSmartObject mqttDeviceSmartObject = new MqttDeviceSmartObject();
                        mqttDeviceSmartObject.init(deviceId,
                                mqttClient, new HashMap<String, SmartObjectResource<?>>(){
                            {
                                put("energy", new EnergyRawSensor());
                                put("temperature", new TemperatureRawSensor());
                            }
                        }, true);

                        mqttDeviceSmartObject.start();

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }).start();
    }

}
