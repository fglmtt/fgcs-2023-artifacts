package it.unimore.dipi.iot.smartobject.process;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.io.Receiver;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import it.unimore.dipi.iot.smartobject.resource.AggregatedDeviceStateResource;
import it.unimore.dipi.iot.smartobject.resource.EnergyRawSensor;
import it.unimore.dipi.iot.smartobject.resource.SmartObjectResource;
import it.unimore.dipi.iot.smartobject.resource.TemperatureRawSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class HttpConfigurationHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpConfigurationHandler.class);

    private ObjectMapper objectMapper;

    private DeviceConfiguration deviceConfiguration;

    private Map<String, SmartObjectResource<?>> resourceMap;

    public HttpConfigurationHandler(DeviceConfiguration deviceConfiguration, Map<String, SmartObjectResource<?>> resourceMap) {
        super();
        objectMapper = new ObjectMapper();
        this.deviceConfiguration = deviceConfiguration;
        this.resourceMap = resourceMap;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        try {

            logger.info("HTTP HANDLER -> CONFIGURATION REQUEST: {}", exchange.getRequestMethod());

            if(exchange.getRequestMethod().equals(Methods.GET)) {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(objectMapper.writeValueAsString(deviceConfiguration));
            }
            else {
                exchange.getRequestReceiver().receiveFullString(new Receiver.FullStringCallback() {
                    @Override
                    public void handle(HttpServerExchange httpServerExchange, String receivedString) {

                        DeviceConfiguration newDeviceConfiguration = null;

                        //De-Serialize received body
                        try {
                            newDeviceConfiguration = objectMapper.readValue(receivedString, DeviceConfiguration.class);
                        } catch (JsonProcessingException e) {
                            logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
                            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                        }

                        if(newDeviceConfiguration != null
                                && newDeviceConfiguration.getUpdatePeriodMs() > 0
                                && newDeviceConfiguration.getUpdateInitialDelayMs() > 0){

                            try{
                                //Update only target fields in the configuration
                                deviceConfiguration.setUpdatePeriodMs(newDeviceConfiguration.getUpdatePeriodMs());
                                deviceConfiguration.setUpdateInitialDelayMs(newDeviceConfiguration.getUpdateInitialDelayMs());
                                deviceConfiguration.setAggregatedTelemetryMsgSec(newDeviceConfiguration.getAggregatedTelemetryMsgSec());

                                for (Map.Entry<String, SmartObjectResource<?>> entry : resourceMap.entrySet()) {
                                    String resourceKey = entry.getKey();
                                    SmartObjectResource<?> resource = entry.getValue();

                                    if(resource.getType().equals(EnergyRawSensor.RESOURCE_TYPE)){

                                        logger.info("Updating Resource {} with new updates intervals values (ms): ({},{})",
                                                resourceKey,
                                                deviceConfiguration.getUpdatePeriodMs(),
                                                deviceConfiguration.getUpdateInitialDelayMs());

                                        EnergyRawSensor energyRawSensor = (EnergyRawSensor)resource;
                                        energyRawSensor.setUpdatePeriodMs(deviceConfiguration.getUpdatePeriodMs());
                                        energyRawSensor.setUpdateInitialDelayMs(deviceConfiguration.getUpdateInitialDelayMs());
                                        energyRawSensor.stopPeriodicSensorUpdate();
                                        energyRawSensor.startPeriodicSensorUpdate();
                                    }
                                    else if(resource.getType().equals(TemperatureRawSensor.RESOURCE_TYPE)) {
                                        TemperatureRawSensor temperatureRawSensor = (TemperatureRawSensor)resource;
                                        temperatureRawSensor.setUpdatePeriodMs(deviceConfiguration.getUpdatePeriodMs());
                                        temperatureRawSensor.setUpdateInitialDelayMs(deviceConfiguration.getUpdateInitialDelayMs());
                                        temperatureRawSensor.stopPeriodicSensorUpdate();
                                        temperatureRawSensor.startPeriodicSensorUpdate();
                                    }
                                    else if(resource.getType().equals(AggregatedDeviceStateResource.RESOURCE_TYPE)) {
                                        AggregatedDeviceStateResource aggregatedDeviceStateResource = (AggregatedDeviceStateResource)resource;
                                        aggregatedDeviceStateResource.setTargetMsgSec(deviceConfiguration.getAggregatedTelemetryMsgSec());
                                        aggregatedDeviceStateResource.setUpdateInitialDelayMs(deviceConfiguration.getUpdateInitialDelayMs());
                                        aggregatedDeviceStateResource.stopPeriodicStateUpdate();
                                        aggregatedDeviceStateResource.startPeriodicStateUpdate();
                                    }
                                }

                                exchange.setStatusCode(StatusCodes.NO_CONTENT);

                            }catch (Exception e){
                                logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
                                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                            }
                        }
                        else {
                            logger.error("HTTP CONFIGURATION HANDLER WRONG RECEIVED CONFIGURATION ! Conf: {}", newDeviceConfiguration);
                            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                        }
                    }
                });
            }
        }catch (Exception e){
            logger.error("HTTP CONFIGURATION HANDLER Exception ! Msg: {}", e.getLocalizedMessage());
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        }

    }
}