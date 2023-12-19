package it.unimore.dipi.iot.smartobject.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project iiot-device
 * @created 27/09/2022 - 11:32
 */
public class AggregatedDeviceStateResource extends SmartObjectResource<AggregatedResourcePayload> implements ResourceDataListener<Object> {

    private static Logger logger = LoggerFactory.getLogger(AggregatedDeviceStateResource.class);

    public static final String RESOURCE_TYPE = "iot.sensor.state.aggregated";

    private List<SmartObjectResource<?>> resourceList;

    private AggregatedResourcePayload aggregatedResourcePayload = null;

    private Timer updateTimer = null;

    private double targetMsgSec;

    private long updatePeriodMs;

    private long updateInitialDelayMs;

    //Ms associated to data update
    public static final double DEFAULT_MSG_RATE_SEC = 1.0;

    private static final long DEFAULT_TASK_DELAY_TIME_MS = 5000;

    private AggregatedDeviceStateResource() {}

    public AggregatedDeviceStateResource(SmartObjectResource<?>... resourceArray) {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.targetMsgSec = DEFAULT_MSG_RATE_SEC;
        this.updateInitialDelayMs = DEFAULT_TASK_DELAY_TIME_MS;
        this.resourceList = Arrays.asList(resourceArray);
        init();
    }

    public AggregatedDeviceStateResource(double targetMsgSec, long updateInitialDelayMs, SmartObjectResource<?>... resourceArray) {
        this(resourceArray);
        this.targetMsgSec = targetMsgSec;
        this.updateInitialDelayMs = updateInitialDelayMs;
        init();
    }

    public AggregatedDeviceStateResource(double targetMsgSec, long updateInitialDelayMs, List<SmartObjectResource<?>> resourceList) {
        super(UUID.randomUUID().toString(), RESOURCE_TYPE);
        this.resourceList = resourceList;
        this.targetMsgSec = targetMsgSec;
        this.updateInitialDelayMs = updateInitialDelayMs;
        init();
    }


    private void init(){

        aggregatedResourcePayload = new AggregatedResourcePayload();

        for(SmartObjectResource<?> smartObjectResource : resourceList)
            ((SmartObjectResource<Object>)smartObjectResource).addDataListener(this);

        startPeriodicStateUpdate();
    }

    @Override
    public AggregatedResourcePayload loadUpdatedValue() {
        return this.aggregatedResourcePayload;
    }

    @Override
    public void onDataChanged(SmartObjectResource<Object> resource, Object updatedValue) {
        aggregatedResourcePayload.put(resource.getType(), updatedValue);
    }

    public void startPeriodicStateUpdate(){

        try{

            this.updatePeriodMs = (long)(1000.0 * (1.0 / this.targetMsgSec));

            logger.info("Wait Initial Delay {} & Starting periodic Update Task with Target Msg/Rate: {} sec - Update Period: {} ms",
                    this.updateInitialDelayMs,
                    this.targetMsgSec,
                    this.updatePeriodMs);

            this.updateTimer = new Timer();
            this.updateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    notifyUpdate(aggregatedResourcePayload);
                }
            }, this.updateInitialDelayMs, this.updatePeriodMs);

        }catch (Exception e){
            logger.error("Error executing periodic resource value ! Msg: {}", e.getLocalizedMessage());
        }

    }

    public void stopPeriodicStateUpdate(){

        try{
            logger.info("Stopping Sensor Update Task ...");

            this.updateTimer.cancel();
            this.updateTimer.purge();
            this.updateTimer = null;

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public List<SmartObjectResource<?>> getResourceList() {
        return resourceList;
    }

    public void setResourceList(List<SmartObjectResource<?>> resourceList) {
        this.resourceList = resourceList;
    }

    public AggregatedResourcePayload getAggregatedResourcePayload() {
        return aggregatedResourcePayload;
    }

    public void setAggregatedResourcePayload(AggregatedResourcePayload aggregatedResourcePayload) {
        this.aggregatedResourcePayload = aggregatedResourcePayload;
    }

    public double getTargetMsgSec() {
        return targetMsgSec;
    }

    public void setTargetMsgSec(double targetMsgSec) {
        this.targetMsgSec = targetMsgSec;
    }

    public long getUpdatePeriodMs() {
        return updatePeriodMs;
    }

    public long getUpdateInitialDelayMs() {
        return updateInitialDelayMs;
    }

    public void setUpdateInitialDelayMs(long updateInitialDelayMs) {
        this.updateInitialDelayMs = updateInitialDelayMs;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("AggregatedDeviceStateResource{");
        sb.append("resourceList=").append(resourceList);
        sb.append(", aggregatedResourcePayload=").append(aggregatedResourcePayload);
        sb.append(", updateTimer=").append(updateTimer);
        sb.append(", targetMsgSec=").append(targetMsgSec);
        sb.append(", updatePeriodMs=").append(updatePeriodMs);
        sb.append(", updateInitialDelayMs=").append(updateInitialDelayMs);
        sb.append(", resourceListenerList=").append(resourceListenerList);
        sb.append('}');
        return sb.toString();
    }
}
