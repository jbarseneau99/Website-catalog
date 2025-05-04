package com.spacedataarchive.monitoring;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents a Kubernetes resource such as a Pod, Service, etc.
 */
public class KubernetesResource {
    
    private final StringProperty name;
    private final StringProperty type;
    private final StringProperty status;
    private final StringProperty age;
    private final StringProperty container;
    
    /**
     * Constructs a new KubernetesResource.
     * 
     * @param name The name of the resource
     * @param type The type of resource (Pod, Service, etc.)
     * @param status The current status
     * @param age How long the resource has been running
     */
    public KubernetesResource(String name, String type, String status, String age) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.status = new SimpleStringProperty(status);
        this.age = new SimpleStringProperty(age);
        this.container = new SimpleStringProperty("N/A");
    }
    
    /**
     * Constructs a new KubernetesResource with container information.
     * 
     * @param name The name of the resource
     * @param type The type of resource (Pod, Service, etc.)
     * @param status The current status
     * @param age How long the resource has been running
     * @param container The container or service this resource is part of
     */
    public KubernetesResource(String name, String type, String status, String age, String container) {
        this.name = new SimpleStringProperty(name);
        this.type = new SimpleStringProperty(type);
        this.status = new SimpleStringProperty(status);
        this.age = new SimpleStringProperty(age);
        this.container = new SimpleStringProperty(container);
    }
    
    /**
     * Gets the name property.
     */
    public StringProperty nameProperty() {
        return name;
    }
    
    /**
     * Gets the type property.
     */
    public StringProperty typeProperty() {
        return type;
    }
    
    /**
     * Gets the status property.
     */
    public StringProperty statusProperty() {
        return status;
    }
    
    /**
     * Gets the age property.
     */
    public StringProperty ageProperty() {
        return age;
    }
    
    /**
     * Gets the container property.
     */
    public StringProperty containerProperty() {
        return container;
    }
    
    /**
     * Gets the name.
     */
    public String getName() {
        return name.get();
    }
    
    /**
     * Gets the type.
     */
    public String getType() {
        return type.get();
    }
    
    /**
     * Gets the status.
     */
    public String getStatus() {
        return status.get();
    }
    
    /**
     * Gets the age.
     */
    public String getAge() {
        return age.get();
    }
    
    /**
     * Gets the container.
     */
    public String getContainer() {
        return container.get();
    }
} 