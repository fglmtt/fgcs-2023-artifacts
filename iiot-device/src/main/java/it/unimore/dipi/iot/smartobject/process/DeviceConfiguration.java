package it.unimore.dipi.iot.smartobject.process;

import java.util.Map;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project wldt-digital-twin-mqtt
 * @created 01/03/2022 - 17:30
 */
public class DeviceConfiguration {

    private String deviceId;

    private String targetMqttBrokerAddress;

    private int targetMqttBrokerPort;

    private long updatePeriodMs;

    private long updateInitialDelayMs;

    private Map<String, String> resourceMap;

    private int httpApiPort = 5555;

    private boolean aggregatedTelemetry = true;

    private double aggregatedTelemetryMsgSec;

    private boolean singleResourceTelemetryEnabled;

    private int targetAggregatedTelemetryPayloadSizeByte;

    public DeviceConfiguration() {
    }

    public DeviceConfiguration(String deviceId, String targetMqttBrokerAddress, int targetMqttBrokerPort) {
        this.deviceId = deviceId;
        this.targetMqttBrokerAddress = targetMqttBrokerAddress;
        this.targetMqttBrokerPort = targetMqttBrokerPort;
    }

    public DeviceConfiguration(String deviceId, String targetMqttBrokerAddress, int targetMqttBrokerPort, long updatePeriodMs, long updateInitialDelayMs, Map<String, String> resourceMap) {
        this.deviceId = deviceId;
        this.targetMqttBrokerAddress = targetMqttBrokerAddress;
        this.targetMqttBrokerPort = targetMqttBrokerPort;
        this.updatePeriodMs = updatePeriodMs;
        this.updateInitialDelayMs = updateInitialDelayMs;
        this.resourceMap = resourceMap;
    }

    public boolean getAggregatedTelemetry() {
        return aggregatedTelemetry;
    }

    public void setAggregatedTelemetry(boolean aggregatedTelemetry) {
        this.aggregatedTelemetry = aggregatedTelemetry;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTargetMqttBrokerAddress() {
        return targetMqttBrokerAddress;
    }

    public void setTargetMqttBrokerAddress(String targetMqttBrokerAddress) {
        this.targetMqttBrokerAddress = targetMqttBrokerAddress;
    }

    public int getTargetMqttBrokerPort() {
        return targetMqttBrokerPort;
    }

    public void setTargetMqttBrokerPort(int targetMqttBrokerPort) {
        this.targetMqttBrokerPort = targetMqttBrokerPort;
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

    public Map<String, String> getResourceMap() {
        return resourceMap;
    }

    public void setResourceMap(Map<String, String> resourceMap) {
        this.resourceMap = resourceMap;
    }

    public int getHttpApiPort() {
        return httpApiPort;
    }

    public void setHttpApiPort(int httpApiPort) {
        this.httpApiPort = httpApiPort;
    }

    public double getAggregatedTelemetryMsgSec() {
        return aggregatedTelemetryMsgSec;
    }

    public void setAggregatedTelemetryMsgSec(double aggregatedTelemetryMsgSec) {
        this.aggregatedTelemetryMsgSec = aggregatedTelemetryMsgSec;
    }

    public boolean getSingleResourceTelemetryEnabled() {
        return singleResourceTelemetryEnabled;
    }

    public void setSingleResourceTelemetryEnabled(boolean singleResourceTelemetry) {
        this.singleResourceTelemetryEnabled = singleResourceTelemetry;
    }

    public int getTargetAggregatedTelemetryPayloadSizeByte() {
        return targetAggregatedTelemetryPayloadSizeByte;
    }

    public void setTargetAggregatedTelemetryPayloadSizeByte(int targetAggregatedTelemetryPayloadSizeByte) {
        this.targetAggregatedTelemetryPayloadSizeByte = targetAggregatedTelemetryPayloadSizeByte;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DeviceConfiguration{");
        sb.append("deviceId='").append(deviceId).append('\'');
        sb.append(", targetMqttBrokerAddress='").append(targetMqttBrokerAddress).append('\'');
        sb.append(", targetMqttBrokerPort=").append(targetMqttBrokerPort);
        sb.append(", updatePeriodMs=").append(updatePeriodMs);
        sb.append(", updateInitialDelayMs=").append(updateInitialDelayMs);
        sb.append(", resourceMap=").append(resourceMap);
        sb.append(", httpApiPort=").append(httpApiPort);
        sb.append(", aggregatedTelemetry=").append(aggregatedTelemetry);
        sb.append(", aggregatedTelemetryMsgSec=").append(aggregatedTelemetryMsgSec);
        sb.append(", singleResourceTelemetryEnabled=").append(singleResourceTelemetryEnabled);
        sb.append(", targetAggregatedTelemetryPayloadSizeByte=").append(targetAggregatedTelemetryPayloadSizeByte);
        sb.append('}');
        return sb.toString();
    }
}
