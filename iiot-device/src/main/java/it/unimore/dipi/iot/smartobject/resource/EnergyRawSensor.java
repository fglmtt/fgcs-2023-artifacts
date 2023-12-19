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
public class EnergyRawSensor extends SmartObjectResource<Double> {

    private static Logger logger = LoggerFactory.getLogger(EnergyRawSensor.class);

    //kWh - kilowatt-hour
    private static final double MIN_ENERGY_VALUE = 5.0;

    //kWh - kilowatt-hour
    private static final double MAX_ENERGY_VALUE = 1.0;

    //kWh - kilowatt-hour
    private static final double MIN_ENERGY_VARIATION = 0.1;

    //kWh - kilowatt-hour
    private static final double MAX_ENERGY_VARIATION = 0.5;

    private static final String LOG_DISPLAY_NAME = "EnergySensor";

    //Ms associated to data update
    private static final long DEFAULT_UPDATE_PERIOD_MS = 1000;

    private static final long DEFAULT_TASK_DELAY_TIME_MS = 5000;

    public static final String RESOURCE_TYPE = "iot.sensor.energy";

    private Double updatedValue;

    private Random random;

    private Timer updateTimer = null;

    private boolean isActive = true;

    private long updatePeriodMs;

    private long updateInitialDelayMs;

    public EnergyRawSensor() {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.updatePeriodMs = DEFAULT_UPDATE_PERIOD_MS;
        this.updateInitialDelayMs = DEFAULT_TASK_DELAY_TIME_MS;
        init();
    }

    public EnergyRawSensor(long updatePeriodMs, long updateInitialDelayMs) {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.updatePeriodMs = updatePeriodMs;
        this.updateInitialDelayMs = updateInitialDelayMs;
        init();
    }

    private void init(){

        try{
            this.random = new Random(System.currentTimeMillis());
            this.updatedValue = MIN_ENERGY_VALUE + this.random.nextDouble()*(MAX_ENERGY_VALUE - MIN_ENERGY_VALUE);

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

                    if(isActive){
                        double variation = (MIN_ENERGY_VARIATION + MAX_ENERGY_VARIATION *random.nextDouble()) * (random.nextDouble() > 0.5 ? 1.0 : -1.0);
                        updatedValue = updatedValue + variation;
                    }
                    else
                        updatedValue = 0.0;

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

    @Override
    public Double loadUpdatedValue() {
        return this.updatedValue;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public static void main(String[] args) {

        EnergyRawSensor rawResource = new EnergyRawSensor();
        rawResource.setActive(false);
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
