package it.dt.function.test;

import it.dt.function.config.FunctionConfiguration;
import org.apache.commons.io.FileUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Base64;
import java.util.UUID;

/**
 * Simple MQTT Consumer using the library Eclipse Paho
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project mqtt-playground
 * @created 14/10/2020 - 09:19
 */
public class ImageConsumer {

    private final static Logger logger = LoggerFactory.getLogger(ImageConsumer.class);

    public static void main(String [ ] args) {

    	logger.info("MQTT Consumer Tester Started ...");

        try{

            //Read Configuration
            FunctionConfiguration functionConfiguration = FunctionConfiguration.readConfigurationFile();

            if(functionConfiguration == null) {
                logger.error("Error Loading Configuration !");
                System.exit(-1);
            }

            String clientId = UUID.randomUUID().toString();

            MqttClientPersistence persistence = new MemoryPersistence();

            IMqttClient client = new MqttClient(
                    String.format("tcp://%s:%d", functionConfiguration.getBrokerIp(), functionConfiguration.getBrokerPort()), //Create the URL from IP and PORT
                    clientId,
                    persistence);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            client.connect(options);

            logger.info("Connected ! Client Id: {}", clientId);

            client.subscribe(FunctionConfiguration.FLUID_PHYSICAL_IMAGE_TOPIC, (topic, msg) -> {
                //The topic variable contain the specific topic associated to the received message. Using MQTT wildcards
                //messaged from multiple and different topic can be received with the same subscription
                //The msg variable is a MqttMessage object containing all the information about the received message
                byte[] payload = msg.getPayload();
                byte[] decodedBytes = Base64.getDecoder().decode(payload);

                String outputFileName = String.format("data/%d.png", System.currentTimeMillis());
                FileUtils.writeByteArrayToFile(new File(outputFileName), decodedBytes);

                logger.info("Message Received ({}) Image Written !", topic);
            });

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
