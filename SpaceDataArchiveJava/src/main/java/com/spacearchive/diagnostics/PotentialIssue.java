package com.spacearchive.diagnostics;

import java.util.List;
import java.util.Map;

/**
 * Represents a potential issue identified during URL diagnostics
 */
public class PotentialIssue {
    private String type;
    private String description;
    private List<String> iframeUrls;
    private Map<String, Integer> containers;
    
    /**
     * Default constructor
     */
    public PotentialIssue() {
    }
    
    /**
     * Constructor with initial values
     * 
     * @param type Issue type
     * @param description Issue description
     */
    public PotentialIssue(String type, String description) {
        this.type = type;
        this.description = description;
    }
    
    /**
     * Constructor with all values
     * 
     * @param type Issue type
     * @param description Issue description
     * @param iframeUrls List of iframe URLs
     * @param containers Map of container types to counts
     */
    public PotentialIssue(String type, String description, List<String> iframeUrls, Map<String, Integer> containers) {
        this.type = type;
        this.description = description;
        this.iframeUrls = iframeUrls;
        this.containers = containers;
    }
    
    // Getters and Setters
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getIframeUrls() {
        return iframeUrls;
    }
    
    public void setIframeUrls(List<String> iframeUrls) {
        this.iframeUrls = iframeUrls;
    }
    
    public Map<String, Integer> getContainers() {
        return containers;
    }
    
    public void setContainers(Map<String, Integer> containers) {
        this.containers = containers;
    }
} 