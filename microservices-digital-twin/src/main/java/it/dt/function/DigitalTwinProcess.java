package it.dt.function;

import it.dt.function.config.FunctionConfiguration;
import it.dt.function.logger.MyDefaultWldtEventLogger;
import it.dt.function.shadowing.FunctionDrivenShadowing;
import it.wldt.adapter.mqtt.digital.MqttDigitalAdapter;
import it.wldt.adapter.mqtt.digital.MqttDigitalAdapterConfiguration;
import it.wldt.adapter.mqtt.digital.topic.MqttQosLevel;
import it.wldt.adapter.mqtt.physical.MqttPhysicalAdapter;
import it.wldt.adapter.mqtt.physical.MqttPhysicalAdapterConfiguration;
import it.wldt.core.engine.WldtEngine;
import it.wldt.core.event.WldtEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 17/11/2023 - 14:42
 */
public class DigitalTwinProcess {

    private final static Logger logger = LoggerFactory.getLogger(DigitalTwinProcess.class);

    public static void main(String[] args) {

        try{

            //Read Configuration
            FunctionConfiguration functionConfiguration = FunctionConfiguration.readConfigurationFile();

            if(functionConfiguration == null) {
                logger.error("Error Loading Configuration !");
                System.exit(-1);
            }

            WldtEngine wldtEngine = new WldtEngine(new FunctionDrivenShadowing(), "fluid-digital-twin");

            MqttPhysicalAdapterConfiguration config = MqttPhysicalAdapterConfiguration.builder(functionConfiguration.getBrokerIp(), functionConfiguration.getBrokerPort())
                    .addPhysicalAssetPropertyAndTopic(
                            "image",
                            "",
                            FunctionConfiguration.FLUID_PHYSICAL_IMAGE_TOPIC,
                            Function.identity())
                    .build();

            MqttDigitalAdapterConfiguration configuration = MqttDigitalAdapterConfiguration.builder(functionConfiguration.getBrokerIp(), functionConfiguration.getBrokerPort())
                    .addPropertyTopic(
                            FunctionConfiguration.DT_RESULT_PROPERTY,
                            buildDigitalTwinTopic(functionConfiguration.getDigitalTwinId(), FunctionConfiguration.FLUID_DT_RESULT_TOPIC),
                            MqttQosLevel.MQTT_QOS_0,
                            Function.identity())
                    .addActionTopic(
                            FunctionConfiguration.DT_FUNCTION_ACTION_KEY,
                            buildDigitalTwinTopic(functionConfiguration.getDigitalTwinId(), FunctionConfiguration.FLUID_DT_FUNCTION_ACTION_TOPIC),
                            Function.identity())
                    .build();

            wldtEngine.addDigitalAdapter(new MqttDigitalAdapter("test-da", configuration));
            wldtEngine.addPhysicalAdapter(new MqttPhysicalAdapter("fluid-mqtt-pa", config));

            //Configure Custom Event Logger to avoid to print bse64 Image Encoding
            WldtEventBus.getInstance().setEventLogger(new MyDefaultWldtEventLogger());

            //Add a LifeCycle Listener to monitor the behaviour of the Twin and notify via MQTT
            wldtEngine.addLifeCycleListener(new MqttLifeCycleListener());

            wldtEngine.startLifeCycle();

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String buildDigitalTwinTopic(String digitalTwinId, String targetTopic){
        return String.format("dt/%s/%s", digitalTwinId, targetTopic);
    }

}
