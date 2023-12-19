package it.dt.function.test;

import it.dt.function.config.FunctionConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Base64;
import java.util.UUID;

/**
 * Simple MQTT Producer using the library Eclipse Paho
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project mqtt-playground
 * @created 14/10/2020 - 09:19
 */
public class ImageProducer {

    private final static Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    private static String IMAGE_FILE_PATH = "data/test_image.png";

    private static long SLEEP_TIME_MS = 500;

    private static int MESSAGE_COUNT = 1000000;

    public static void main(String[] args) {

        logger.info("ImageProducer started ...");

        try{

            //Read Configuration
            FunctionConfiguration functionConfiguration = FunctionConfiguration.readConfigurationFile();

            if(functionConfiguration == null) {
                logger.error("Error Loading Configuration !");
                System.exit(-1);
            }

            String mqttClientId = UUID.randomUUID().toString();

            MqttClientPersistence persistence = new MemoryPersistence();

            IMqttClient client = new MqttClient(
                    String.format("tcp://%s:%d", functionConfiguration.getBrokerIp(), functionConfiguration.getBrokerPort()),
                    mqttClientId,
                    persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            //Connect to the target broker
            client.connect(options);

            logger.info("Connected ! Client Id: {}", mqttClientId);

            String targetImageBase64 = readImageBase64();

            //Start to publish MESSAGE_COUNT messages
            for(int i = 0; i < MESSAGE_COUNT; i++) {

            	//Internal Method to publish MQTT data using the created MQTT Client
            	publishData(client, FunctionConfiguration.FLUID_PHYSICAL_IMAGE_TOPIC, targetImageBase64);

            	//Sleep for 1 Second
            	Thread.sleep(SLEEP_TIME_MS);
            }

            //Disconnect from the broker and close the connection
            client.disconnect();
            client.close();

            logger.info("Disconnected !");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static String readImageBase64(){
        try{
            byte[] fileContent = FileUtils.readFileToByteArray(new File(IMAGE_FILE_PATH));
            return Base64.getEncoder().encodeToString(fileContent);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Send a target String Payload to the specified MQTT topic
     *
     * @param mqttClient
     * @param topic
     * @param msgString
     * @throws MqttException
     */
    public static void publishData(IMqttClient mqttClient,
                                   String topic,
                                   String msgString) throws MqttException {

        if (mqttClient.isConnected() && msgString != null && topic != null) {

            //Create an MQTT Message defining the required QoS Level and if the message is retained or not
            MqttMessage msg = new MqttMessage(msgString.getBytes());
            msg.setQos(0);
            msg.setRetained(false);

            mqttClient.publish(topic,msg);

        }
        else{
            logger.error("Error: Topic or Msg = Null or MQTT Client is not Connected !");
        }

    }



}
