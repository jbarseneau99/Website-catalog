package com.spacedataarchive.model;

/**
 * Represents a system performance metric.
 */
public class SystemMetric {
    private String name;
    private String value;
    
    /**
     * Default constructor.
     */
    public SystemMetric() {
    }
    
    /**
     * Constructor with parameters.
     * 
     * @param name The name of the metric
     * @param value The value of the metric
     */
    public SystemMetric(String name, String value) {
        this.name = name;
        this.value = value;
    }
    
    /**
     * Gets the name.
     * 
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name.
     * 
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the value.
     * 
     * @return The value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Sets the value.
     * 
     * @param value The value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public String toString() {
        return "SystemMetric{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
} 