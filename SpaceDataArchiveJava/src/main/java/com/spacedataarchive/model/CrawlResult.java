package com.spacedataarchive.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a result from a web crawl operation.
 */
public class CrawlResult {
    private String url;
    private String title;
    private String status;
    private ValidationResult validationResult;
    private Map<String, Object> metadata;
    
    /**
     * Default constructor.
     */
    public CrawlResult() {
        metadata = new HashMap<>();
    }
    
    /**
     * Constructor with parameters.
     * 
     * @param url The URL that was crawled
     * @param title The title of the web page
     * @param status The status of the crawl operation
     */
    public CrawlResult(String url, String title, String status) {
        this.url = url;
        this.title = title;
        this.status = status;
        this.metadata = new HashMap<>();
    }
    
    /**
     * Gets the URL.
     * 
     * @return The URL
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * Sets the URL.
     * 
     * @param url The URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Gets the title.
     * 
     * @return The title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the title.
     * 
     * @param title The title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Gets the status.
     * 
     * @return The status
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets the status.
     * 
     * @param status The status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Gets the validation result.
     * 
     * @return The validation result
     */
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Sets the validation result.
     * 
     * @param validationResult The validation result to set
     */
    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }
    
    /**
     * Gets the metadata.
     * 
     * @return The metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Sets the metadata.
     * 
     * @param metadata The metadata to set
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Adds a metadata entry.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, Object value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(key, value);
    }
    
    @Override
    public String toString() {
        return "CrawlResult{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", validationResult=" + validationResult +
                ", metadata=" + metadata +
                '}';
    }
} 