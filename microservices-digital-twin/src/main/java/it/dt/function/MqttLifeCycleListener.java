package it.dt.function;

import it.dt.function.config.FunctionConfiguration;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.core.engine.LifeCycleListener;
import it.wldt.core.state.IDigitalTwinState;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.UUID;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 23/11/2023 - 15:44
 */
public class MqttLifeCycleListener implements LifeCycleListener {

    private final static Logger logger = LoggerFactory.getLogger(MqttLifeCycleListener.class);

    private IMqttClient mqttClient;

    private FunctionConfiguration functionConfiguration;

    public static final String LIFE_CYCLE_STATE_ON_SYNC = "ON-SYNC";

    public MqttLifeCycleListener() {

        try{

            //Read Configuration
            functionConfiguration = FunctionConfiguration.readConfigurationFile();

            if(functionConfiguration == null) {
                logger.error("Error Loading Configuration !");
            }
            else {

                String mqttClientId = UUID.randomUUID().toString();

                MqttClientPersistence persistence = new MemoryPersistence();

                this.mqttClient = new MqttClient(
                        String.format("tcp://%s:%d", functionConfiguration.getBrokerIp(), functionConfiguration.getBrokerPort()),
                        mqttClientId,
                        persistence);

                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setConnectionTimeout(10);

                //Connect to the target broker
                mqttClient.connect(options);

                logger.info("Connected ! Client Id: {}", mqttClientId);
            }

        }catch (Exception e){
            e.printStackTrace();
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

    @Override
    public void onCreate() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onPhysicalAdapterBound(String s, PhysicalAssetDescription physicalAssetDescription) {

    }

    @Override
    public void onPhysicalAdapterBindingUpdate(String s, PhysicalAssetDescription physicalAssetDescription) {

    }

    @Override
    public void onPhysicalAdapterUnBound(String s, PhysicalAssetDescription physicalAssetDescription, String s1) {

    }

    @Override
    public void onDigitalAdapterBound(String s) {

    }

    @Override
    public void onDigitalAdapterUnBound(String s, String s1) {

    }

    @Override
    public void onDigitalTwinBound(Map<String, PhysicalAssetDescription> map) {

    }

    @Override
    public void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String s) {

    }

    @Override
    public void onSync(IDigitalTwinState digitalTwinState) {
        try{

            logger.info("MqttLifeCycleListener -> onSync() - DT State: {}", digitalTwinState);

            if(this.mqttClient != null && this.mqttClient.isConnected()){

                String targetTopic = String.format("dt/%s/fluid/dt/lifecycle", this.functionConfiguration.getDigitalTwinId());

                //TODO Status Payload can be improved !
                publishData(this.mqttClient, targetTopic, LIFE_CYCLE_STATE_ON_SYNC);

                //Disconnect from the broker and close the connection
                //mqttClient.disconnect();
                //mqttClient.close();

            }
            else
                logger.warn("MQTT Client = null or DISCONNECTED ! Nothing to do ...");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onUnSync(IDigitalTwinState digitalTwinState) {
    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }
}
