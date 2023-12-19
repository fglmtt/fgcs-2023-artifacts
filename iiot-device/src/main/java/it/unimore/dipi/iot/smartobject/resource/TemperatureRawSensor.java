package it.unimore.dipi.iot.smartobject.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smarthome
 * @created 11/11/2020 - 14:43
 */
public class TemperatureRawSensor extends SmartObjectResource<Double>{

    private static Logger logger = LoggerFactory.getLogger(TemperatureRawSensor.class);

    private static final double MIN_TEMPERATURE_VALUE = 25.0;

    private static final double MAX_TEMPERATURE_VALUE = 30.0;

    private static final double MIN_TEMPERATURE_VARIATION = 0.1;

    private static final double MAX_TEMPERATURE_VARIATION = 1.0;

    private static final String LOG_DISPLAY_NAME = "TemperatureSensor";

    //Ms associated to data update
    public static final long DEFAULT_UPDATE_PERIOD_MS = 1000;

    private static final long DEFAULT_TASK_DELAY_TIME_MS = 5000;

    public static final String RESOURCE_TYPE = "iot.sensor.temperature";

    private Double updatedValue;

    private Random random;

    private Timer updateTimer = null;

    private long updatePeriodMs;

    private long updateInitialDelayMs;

    public TemperatureRawSensor() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.updatePeriodMs = DEFAULT_UPDATE_PERIOD_MS;
        this.updateInitialDelayMs = DEFAULT_TASK_DELAY_TIME_MS;
        init();
    }

    public TemperatureRawSensor(long updatePeriodMs, long updateInitialDelayMs) {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.updatePeriodMs = updatePeriodMs;
        this.updateInitialDelayMs = updateInitialDelayMs;
        init();
    }

    private void init(){

        try{

            this.random = new Random(System.currentTimeMillis());
            this.updatedValue = MIN_TEMPERATURE_VALUE + this.random.nextDouble()*(MAX_TEMPERATURE_VALUE - MIN_TEMPERATURE_VALUE);

            startPeriodicSensorUpdate();

        }catch (Exception e){
            logger.error("Error initializing the IoT Resource ! Msg: {}", e.getLocalizedMessage());
        }

    }

    public void startPeriodicSensorUpdate(){

        try{

            logger.info("Wait Initial Delay {} & Starting periodic Update Task with Period: {} ms",
                    this.updateInitialDelayMs,
                    this.updatePeriodMs);

            this.updateTimer = new Timer();
            this.updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {

                    double variation = (MIN_TEMPERATURE_VARIATION + MAX_TEMPERATURE_VARIATION*random.nextDouble()) * (random.nextDouble() > 0.5 ? 1.0 : -1.0);
                    updatedValue = updatedValue + variation;
                    notifyUpdate(updatedValue);

                }
            }, this.updateInitialDelayMs, this.updatePeriodMs);

        }catch (Exception e){
            logger.error("Error executing periodic resource value ! Msg: {}", e.getLocalizedMessage());
        }

    }

    public void stopPeriodicSensorUpdate(){

        try{
            logger.info("Stopping Sensor Update Task ...");

            this.updateTimer.cancel();
            this.updateTimer.purge();
            this.updateTimer = null;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public Double loadUpdatedValue() {
        return this.updatedValue;
    }

    public long getUpdatePeriodMs() {
        return updatePeriodMs;
    }

    public void setUpdatePeriodMs(long updatePeriodMs) {
        this.updatePeriodMs = updatePeriodMs;
    }

    public long getUpdateInitialDelayMs() {
        return updateInitialDelayMs;
    }

    public void setUpdateInitialDelayMs(long updateInitialDelayMs) {
        this.updateInitialDelayMs = updateInitialDelayMs;
    }

    public static void main(String[] args) {

        TemperatureRawSensor rawResource = new TemperatureRawSensor();
        logger.info("New {} Resource Created with Id: {} ! {} New Value: {}",
                rawResource.getType(),
                rawResource.getId(),
                LOG_DISPLAY_NAME,
                rawResource.loadUpdatedValue());

        rawResource.addDataListener(new ResourceDataListener<Double>() {
            @Override
            public void onDataChanged(SmartObjectResource<Double> resource, Double updatedValue) {

                if(resource != null && updatedValue != null)
                    logger.info("Device: {} -> New Value Received: {}", resource.getId(), updatedValue);
                else
                    logger.error("onDataChanged Callback -> Null Resource or Updated Value !");
            }
        });

    }

}
