package it.dt.function.orchestrator;

import it.dt.function.augmentation.CommandLineResult;
import it.dt.function.config.FunctionConfiguration;
import it.dt.function.test.ImageProducer;
import it.dt.function.augmentation.CommandLineExecutor;
import it.dt.function.augmentation.LinuxProcessExecutor;
import org.apache.commons.io.FileUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 23/11/2023 - 11:32
 */
public class DemoScenarioOrchestrator {

    private final static Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    private static final String TARGET_BROKER_IP = "192.168.1.17";

    private static final int TARGET_BROKER_PORT = 1883;

    private static final String DEFAULT_ORCHESTRATOR_PATH = "orchestrator";

    private static final String DEFAULT_DT_CONTAINER_VERSION = "0.1";

    private static final String DT_LIFE_CYCLE_TOPIC = "dt/+/fluid/dt/lifecycle";

    private static long startTimestamp;

    private static long containerStartedTimestamp;

    private static long dtSynchronizedTimestamp;

    public static void main(String[] args) {
        try{

            //checkCurrentWorkingDirectory();

            Optional<FunctionConfiguration> confDt1Optional = buildDigitalTwinConfiguration("fluidDT-1");
            Optional<FunctionConfiguration> confDt2Optional = buildDigitalTwinConfiguration("fluidDT-2");
            Optional<FunctionConfiguration> confDt3Optional = buildDigitalTwinConfiguration("fluidDT-3");
            Optional<FunctionConfiguration> confDt4Optional = buildDigitalTwinConfiguration("fluidDT-4");

            if(confDt1Optional.isPresent() &&
                    confDt2Optional.isPresent() &&
                    confDt3Optional.isPresent() &&
                    confDt4Optional.isPresent()) {

                FunctionConfiguration confDt1 = confDt1Optional.get();
                FunctionConfiguration confDt2 = confDt2Optional.get();
                FunctionConfiguration confDt3 = confDt3Optional.get();
                FunctionConfiguration confDt4 = confDt4Optional.get();

                Thread.sleep(10*1000);

                logger.info("Starting Fluid Digital Twin with id: {}", confDt1.getDigitalTwinId());
                startDigitalTwin(confDt1.getDigitalTwinId(), DEFAULT_DT_CONTAINER_VERSION);
                Thread.sleep(10*1000);
                sendNewFunction(confDt1.getDigitalTwinId(), "functions/test_function1.py");

                Thread.sleep(10*1000);

                logger.info("Starting Fluid Digital Twin with id: {}", confDt2.getDigitalTwinId());
                startDigitalTwin(confDt2.getDigitalTwinId(), DEFAULT_DT_CONTAINER_VERSION);
                Thread.sleep(10*1000);
                sendNewFunction(confDt2.getDigitalTwinId(), "functions/test_function1.py");

                Thread.sleep(5*60*1000);

                logger.info("Starting Fluid Digital Twin with id: {}", confDt3.getDigitalTwinId());
                startDigitalTwin(confDt3.getDigitalTwinId(), DEFAULT_DT_CONTAINER_VERSION);
                Thread.sleep(10*1000);
                sendNewFunction(confDt3.getDigitalTwinId(), "functions/test_function2.py");

                Thread.sleep(5*60*1000);

                logger.info("Starting Fluid Digital Twin with id: {}", confDt4.getDigitalTwinId());
                startDigitalTwin(confDt4.getDigitalTwinId(), DEFAULT_DT_CONTAINER_VERSION);
                Thread.sleep(10*1000);
                sendNewFunction(confDt4.getDigitalTwinId(), "functions/test_function3.py");

                Thread.sleep(5*60*1000);

                stopDigitalTwin(confDt2.getDigitalTwinId());
                logger.info("Fluid Digital Twin {} STOPPED !", confDt2.getDigitalTwinId());

                Thread.sleep(5*60*1000);

                stopDigitalTwin(confDt3.getDigitalTwinId());
                logger.info("Fluid Digital Twin {} STOPPED !", confDt3.getDigitalTwinId());

                Thread.sleep(5*60*1000);

                stopDigitalTwin(confDt1.getDigitalTwinId());
                logger.info("Fluid Digital Twin {} STOPPED !", confDt4.getDigitalTwinId());

                stopDigitalTwin(confDt4.getDigitalTwinId());
                logger.info("Fluid Digital Twin {} STOPPED !", confDt4.getDigitalTwinId());

                logger.info("Orchestration Scenario Completed !");

            } else
                logger.error("Error builing Digital Twin Configuration !");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static Optional<FunctionConfiguration> buildDigitalTwinConfiguration(String digitalTwinId) {
        try{

            FunctionConfiguration functionConfiguration = new FunctionConfiguration();
            functionConfiguration.setDigitalTwinId(digitalTwinId);
            functionConfiguration.setBrokerIp(TARGET_BROKER_IP);
            functionConfiguration.setBrokerPort(TARGET_BROKER_PORT);

            String targetConfFilePath = String.format("%s/%s.yaml", DEFAULT_ORCHESTRATOR_PATH, digitalTwinId);

            FunctionConfiguration.writeConfigurationFile(targetConfFilePath, functionConfiguration);

            return Optional.of(functionConfiguration);

        }catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static void startDigitalTwin(String targetDtId, String containerVersion){
        try{
            executeCliCommand("./start_target_function_dt.sh", List.of(targetDtId, containerVersion));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void stopDigitalTwin(String targetDtId){
        try{
            executeCliCommand("./stop_target_function_dt.sh", List.of(targetDtId));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void executeCliCommand(String command, List<String> parameterList){

        try{
            CommandLineExecutor commandLineExecutor = new LinuxProcessExecutor();

            CommandLineResult cliResult = commandLineExecutor.executeCommand(command, parameterList, DEFAULT_ORCHESTRATOR_PATH);

            logger.debug("############ Cli Command Line Result #############");
            logger.info("Command Result: {}", cliResult.getExitCode());
            logger.info("Error Log: {}", cliResult.getErrorLog());
            logger.info("Console Log: {}", cliResult.getOutputLog());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void sendNewFunction(String targetDtId, String targetFunctionPath) {

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

            String targetImageBase64 = readTestPythonFunction(targetFunctionPath);

            //Internal Method to publish MQTT data using the created MQTT Client
            publishData(client, String.format("dt/%s/%s", targetDtId, FunctionConfiguration.FLUID_DT_FUNCTION_ACTION_TOPIC), targetImageBase64);

            //Disconnect from the broker and close the connection
            client.disconnect();
            client.close();

            logger.info("Function: {} Sent to DT: {}! -> Disconnected", targetDtId, targetFunctionPath);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static String readTestPythonFunction(String targetFunctionPath){
        try{
            byte[] fileContent = FileUtils.readFileToByteArray(new File(targetFunctionPath));
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
