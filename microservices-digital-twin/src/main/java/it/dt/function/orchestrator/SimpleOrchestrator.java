package it.dt.function.orchestrator;

import it.dt.function.MqttLifeCycleListener;
import it.dt.function.augmentation.CommandLineExecutor;
import it.dt.function.augmentation.CommandLineResult;
import it.dt.function.augmentation.LinuxProcessExecutor;
import it.dt.function.config.FunctionConfiguration;
import it.dt.function.test.ImageProducer;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 23/11/2023 - 11:32
 */
public class SimpleOrchestrator {

    private final static Logger logger = LoggerFactory.getLogger(ImageProducer.class);

    private static final int DT_ID_NUMBER_MIN = 1;

    private static final int DT_ID_NUMBER_MAX = 100;

    private static final String TARGET_BROKER_IP = "192.168.1.114";

    private static final int TARGET_BROKER_PORT = 1883;

    private static final String DEFAULT_ORCHESTRATOR_PATH = "orchestrator";

    private static final String DEFAULT_DT_CONTAINER_VERSION = "0.1";

    private static final String DT_LIFE_CYCLE_TOPIC = "dt/+/fluid/dt/lifecycle";

    private static long startTimestamp;

    private static long containerStartedTimestamp;

    private static long dtSynchronizedTimestamp;

    private static FunctionConfiguration functionConfiguration;

    public static void main(String[] args) {
        try{

            //checkCurrentWorkingDirectory();

            Optional<FunctionConfiguration> fluidConfigurationOptional = buildRandomDigitalTwinConfiguration();

            if(fluidConfigurationOptional.isPresent()) {

                functionConfiguration = fluidConfigurationOptional.get();

                observeMqttLifeCycle();

                startTimestamp = System.currentTimeMillis();

                logger.info("Starting Fluid Digital Twin with id: {}", functionConfiguration.getDigitalTwinId());

                startDigitalTwin(functionConfiguration.getDigitalTwinId(), DEFAULT_DT_CONTAINER_VERSION);

                containerStartedTimestamp = System.currentTimeMillis();

                logger.info("Fluid Digital Twin {} STARTED !", functionConfiguration.getDigitalTwinId());

                Thread.sleep(10*1000);

                logger.info("Stopping Fluid Digital Twin with id: {}", functionConfiguration.getDigitalTwinId());

                stopDigitalTwin(functionConfiguration.getDigitalTwinId());

                logger.info("Fluid Digital Twin {} STOPPED !", functionConfiguration.getDigitalTwinId());

            } else
                logger.error("Error builing Digital Twin Configuration !");

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void observeMqttLifeCycle() {

        logger.info("MQTT Consumer Tester Started ...");

        try{

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

            client.subscribe(DT_LIFE_CYCLE_TOPIC, (topic, msg) -> {

                byte[] payload = msg.getPayload();
                String lifeCycleState = new String(payload);

                if(lifeCycleState.equals(MqttLifeCycleListener.LIFE_CYCLE_STATE_ON_SYNC) && topic.contains(functionConfiguration.getDigitalTwinId())){
                    dtSynchronizedTimestamp = System.currentTimeMillis();

                    long containerStatupTime = containerStartedTimestamp - startTimestamp;
                    long dtStartupTime = dtSynchronizedTimestamp - startTimestamp;

                    logger.info("Container Startup Time: {}", containerStatupTime);
                    logger.info("Digital Twin Startup Time: {}", dtStartupTime);

                }

            });

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static Optional<FunctionConfiguration> buildRandomDigitalTwinConfiguration() {
        try{

            Random random = new Random();
            int randomInt = random.nextInt(DT_ID_NUMBER_MAX - DT_ID_NUMBER_MIN + 1) + DT_ID_NUMBER_MIN;
            String targetDtId = String.format("testDT%d", randomInt);

            FunctionConfiguration functionConfiguration = new FunctionConfiguration();
            functionConfiguration.setDigitalTwinId(targetDtId);
            functionConfiguration.setBrokerIp(TARGET_BROKER_IP);
            functionConfiguration.setBrokerPort(TARGET_BROKER_PORT);

            String targetConfFilePath = String.format("%s/%s.yaml", DEFAULT_ORCHESTRATOR_PATH, targetDtId);

            FunctionConfiguration.writeConfigurationFile(targetConfFilePath, functionConfiguration);

            return Optional.of(functionConfiguration);

        }catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static void checkCurrentWorkingDirectory(){
        try{
            executeCliCommand("pwd", null);
            executeCliCommand("ls", List.of("-la"));
        }catch (Exception e){
            e.printStackTrace();
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

}
