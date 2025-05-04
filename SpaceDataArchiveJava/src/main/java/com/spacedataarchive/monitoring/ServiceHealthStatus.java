package com.spacedataarchive.monitoring;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents the health status of a service.
 */
public class ServiceHealthStatus {
    
    /**
     * Possible health status values.
     */
    public enum Status {
        HEALTHY,    // Service is running and healthy
        DEGRADED,   // Service is running but with issues
        DOWN,       // Service is not running or not accessible
        UNKNOWN     // Status could not be determined
    }
    
    private final String serviceName;
    private final Status status;
    private final String lastChecked;
    private final int port;
    
    /**
     * Constructs a new ServiceHealthStatus with the specified values.
     * 
     * @param serviceName The name of the service
     * @param status The current health status
     * @param lastChecked When the service was last checked
     */
    public ServiceHealthStatus(String serviceName, Status status, String lastChecked) {
        this.serviceName = serviceName;
        this.status = status;
        this.lastChecked = lastChecked;
        this.port = -1;
    }
    
    /**
     * Constructs a new ServiceHealthStatus with the specified values including port.
     * 
     * @param serviceName The name of the service
     * @param status The current health status
     * @param lastChecked When the service was last checked
     * @param port The port number the service is running on
     */
    public ServiceHealthStatus(String serviceName, Status status, String lastChecked, int port) {
        this.serviceName = serviceName;
        this.status = status;
        this.lastChecked = lastChecked;
        this.port = port;
    }
    
    /**
     * Gets the service name.
     */
    public String getServiceName() {
        return serviceName;
    }
    
    /**
     * Gets the current status.
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Gets the last checked timestamp.
     */
    public String getLastChecked() {
        return lastChecked;
    }
    
    /**
     * Gets the port number.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Returns a string representation of the service status.
     */
    @Override
    public String toString() {
        return "ServiceHealthStatus{" +
                "serviceName='" + serviceName + '\'' +
                ", status=" + status +
                ", lastChecked='" + lastChecked + '\'' +
                ", port=" + port +
                '}';
    }
} 