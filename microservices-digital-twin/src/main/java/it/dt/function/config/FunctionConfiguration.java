package it.dt.function.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 17/11/2023 - 15:37
 */
public class FunctionConfiguration {

    private static final String DT_CONFIGURATION_FILE = "dt_conf.yaml";

    private static final String DEFAULT_BROKER_IP = "127.0.0.1";

    private static final int DEFAULT_BROKER_PORT = 1883;

    public static final String DT_RESULT_PROPERTY = "result";

    public static final String DT_FUNCTION_ACTION_KEY = "function.schedule";

    public static String FLUID_PHYSICAL_IMAGE_TOPIC = "fluid/device/sensor/image";

    public static String FLUID_DT_RESULT_TOPIC = "fluid/dt/properties/result";

    public static String FLUID_DT_FUNCTION_ACTION_TOPIC = "fluid/dt/function/schedule";

    private String brokerIp = DEFAULT_BROKER_IP;

    private int brokerPort = DEFAULT_BROKER_PORT;

    private String digitalTwinId = null;

    public FunctionConfiguration() {
    }

    public FunctionConfiguration(String brokerIp, int brokerPort, String digitalTwinId) {
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.digitalTwinId = digitalTwinId;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public void setBrokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        this.brokerPort = brokerPort;
    }

    public String getDigitalTwinId() {
        return digitalTwinId;
    }

    public void setDigitalTwinId(String digitalTwinId) {
        this.digitalTwinId = digitalTwinId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("FunctionConfiguration{");
        sb.append("brokerIp='").append(brokerIp).append('\'');
        sb.append(", brokerPort=").append(brokerPort);
        sb.append(", digitalTwinId='").append(digitalTwinId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static FunctionConfiguration readConfigurationFile() {
        try{
            //ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //File file = new File(classLoader.getResource(WLDT_CONFIGURATION_FILE).getFile());
            File file = new File(DT_CONFIGURATION_FILE);
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            return om.readValue(file, FunctionConfiguration.class);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void writeConfigurationFile(String destinationFilePath, FunctionConfiguration functionConfiguration){
        try{

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            mapper.writeValue(new File(destinationFilePath), functionConfiguration);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
