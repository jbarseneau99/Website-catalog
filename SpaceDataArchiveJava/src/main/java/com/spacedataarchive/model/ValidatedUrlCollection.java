package com.spacedataarchive.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection of validated URLs from site map data.
 * This class is used to store and manage the results of URL validation.
 */
public class ValidatedUrlCollection {
    private String id;                     // Unique identifier for the collection
    private String name;                   // Name of the collection
    private String sourceMapId;            // ID of the source site map
    private LocalDateTime createdAt;       // When the collection was created
    private LocalDateTime updatedAt;       // When the collection was last updated
    private LocalDateTime completedAt;     // When the validation was completed
    private List<EnhancedValidationResult> urls; // List of validated URLs
    private List<String> explicitUrls;     // List of explicit URLs to validate (when not loading from site map)
    private Map<String, Integer> statistics; // Statistics about the validation results
    private String status;                // Status of the validation (e.g., "In Progress", "Completed", "Error")
    private String message;               // Message about the validation (e.g., error message)
    private int totalUrls;                // Total number of URLs in the collection
    private int validUrls;                // Number of valid URLs
    private int invalidUrls;              // Number of invalid URLs
    private int warningUrls;              // Number of URLs with warnings
    
    /**
     * Default constructor.
     */
    public ValidatedUrlCollection() {
        this.id = generateId();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.urls = new ArrayList<>();
        this.explicitUrls = new ArrayList<>();
        this.statistics = new HashMap<>();
        this.status = "Created";
    }
    
    /**
     * Constructor with name and source map ID.
     *
     * @param name The name of the collection
     * @param sourceMapId The ID of the source site map
     */
    public ValidatedUrlCollection(String name, String sourceMapId) {
        this.id = generateId();
        this.name = name;
        this.sourceMapId = sourceMapId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.urls = new ArrayList<>();
        this.explicitUrls = new ArrayList<>();
        this.statistics = new HashMap<>();
        this.status = "Created";
    }
    
    /**
     * Generates a unique ID for the collection.
     * 
     * @return A unique ID
     */
    private String generateId() {
        return "validation-" + System.currentTimeMillis();
    }
    
    /**
     * Adds a validation result to the collection.
     * 
     * @param result The validation result to add
     */
    public void addUrl(EnhancedValidationResult result) {
        if (urls == null) {
            urls = new ArrayList<>();
        }
        
        // Check if URL already exists by ID or URL
        boolean exists = false;
        for (int i = 0; i < urls.size(); i++) {
            EnhancedValidationResult existing = urls.get(i);
            
            // Check by ID first
            if (result.getUrlId() != null && result.getUrlId().equals(existing.getUrlId())) {
                urls.set(i, result); // Replace with updated result
                exists = true;
                break;
            }
            
            // Then check by URL
            if (result.getUrl() != null && result.getUrl().equals(existing.getUrl())) {
                urls.set(i, result); // Replace with updated result
                exists = true;
                break;
            }
        }
        
        // If it doesn't exist, add it
        if (!exists) {
            urls.add(result);
        }
        
        // Update timestamp and statistics
        updatedAt = LocalDateTime.now();
        updateStatistics();
    }
    
    /**
     * Adds a validation result to the collection.
     * Alias for addUrl for better semantics.
     * 
     * @param result The validation result to add
     */
    public void addResult(ValidationResult result) {
        // Convert to EnhancedValidationResult if needed
        if (result instanceof EnhancedValidationResult) {
            addUrl((EnhancedValidationResult) result);
        } else {
            EnhancedValidationResult enhancedResult = new EnhancedValidationResult(
                result.getUrl(),
                result.getStatus(),
                result.getMessage()
            );
            addUrl(enhancedResult);
        }
    }
    
    /**
     * Updates the statistics based on the current validation results.
     */
    private void updateStatistics() {
        if (urls == null) {
            return;
        }
        
        int total = urls.size();
        int valid = 0;
        int invalid = 0;
        int warning = 0;
        
        Map<String, Integer> assetTypes = new HashMap<>();
        
        for (EnhancedValidationResult result : urls) {
            if ("Valid".equals(result.getStatus())) {
                valid++;
            } else if ("Warning".equals(result.getStatus())) {
                warning++;
            } else {
                invalid++;
            }
            
            // Count by asset type
            if (result.getAssetType() != null) {
                assetTypes.put(result.getAssetType(), 
                        assetTypes.getOrDefault(result.getAssetType(), 0) + 1);
            }
        }
        
        statistics.put("total", total);
        statistics.put("valid", valid);
        statistics.put("invalid", invalid);
        statistics.put("warning", warning);
        
        // Update instance variables
        this.totalUrls = total;
        this.validUrls = valid;
        this.invalidUrls = invalid;
        this.warningUrls = warning;
        
        // Add asset type statistics
        for (Map.Entry<String, Integer> entry : assetTypes.entrySet()) {
            statistics.put("assetType_" + entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Gets the ID.
     * 
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the ID.
     * 
     * @param id The ID to set
     */
    public void setId(String id) {
        this.id = id;
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
     * Gets the source map ID.
     * 
     * @return The source map ID
     */
    public String getSourceMapId() {
        return sourceMapId;
    }
    
    /**
     * Gets the site map ID.
     * Alias for getSourceMapId() for API consistency.
     * 
     * @return The site map ID
     */
    public String getSiteMapId() {
        return sourceMapId;
    }
    
    /**
     * Sets the source map ID.
     * 
     * @param sourceMapId The source map ID to set
     */
    public void setSourceMapId(String sourceMapId) {
        this.sourceMapId = sourceMapId;
    }
    
    /**
     * Sets the site map ID.
     * Alias for setSourceMapId() for API consistency and JSON deserialization.
     * 
     * @param siteMapId The site map ID to set
     */
    public void setSiteMapId(String siteMapId) {
        this.sourceMapId = siteMapId;
    }
    
    /**
     * Gets the creation timestamp.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the creation timestamp.
     * 
     * @param createdAt The creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the update timestamp.
     * 
     * @return The update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the update timestamp.
     * 
     * @param updatedAt The update timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the completed timestamp.
     * 
     * @return The completed timestamp
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    /**
     * Sets the completed timestamp.
     * 
     * @param completedAt The completed timestamp to set
     */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    /**
     * Gets the validated URLs.
     * 
     * @return The validated URLs
     */
    public List<EnhancedValidationResult> getUrls() {
        if (urls == null) {
            urls = new ArrayList<>();
        }
        return urls;
    }
    
    /**
     * Sets the validated URLs.
     * 
     * @param urls The validated URLs to set
     */
    public void setUrls(List<EnhancedValidationResult> urls) {
        this.urls = urls;
        updateStatistics();
    }
    
    /**
     * Gets the statistics.
     * 
     * @return The statistics
     */
    public Map<String, Integer> getStatistics() {
        if (statistics == null) {
            statistics = new HashMap<>();
        }
        return statistics;
    }
    
    /**
     * Sets the statistics.
     * 
     * @param statistics The statistics to set
     */
    public void setStatistics(Map<String, Integer> statistics) {
        this.statistics = statistics;
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
     * Gets the message.
     * 
     * @return The message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the message.
     * 
     * @param message The message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the total number of URLs.
     * 
     * @return The total number of URLs
     */
    public int getTotalUrls() {
        return totalUrls;
    }
    
    /**
     * Sets the total number of URLs.
     * 
     * @param totalUrls The total number of URLs to set
     */
    public void setTotalUrls(int totalUrls) {
        this.totalUrls = totalUrls;
    }
    
    /**
     * Gets the number of valid URLs.
     * 
     * @return The number of valid URLs
     */
    public int getValidUrls() {
        return validUrls;
    }
    
    /**
     * Sets the number of valid URLs.
     * 
     * @param validUrls The number of valid URLs to set
     */
    public void setValidUrls(int validUrls) {
        this.validUrls = validUrls;
    }
    
    /**
     * Gets the number of invalid URLs.
     * 
     * @return The number of invalid URLs
     */
    public int getInvalidUrls() {
        return invalidUrls;
    }
    
    /**
     * Sets the number of invalid URLs.
     * 
     * @param invalidUrls The number of invalid URLs to set
     */
    public void setInvalidUrls(int invalidUrls) {
        this.invalidUrls = invalidUrls;
    }
    
    /**
     * Gets the number of URLs with warnings.
     * 
     * @return The number of URLs with warnings
     */
    public int getWarningUrls() {
        return warningUrls;
    }
    
    /**
     * Sets the number of URLs with warnings.
     * 
     * @param warningUrls The number of URLs with warnings to set
     */
    public void setWarningUrls(int warningUrls) {
        this.warningUrls = warningUrls;
    }
    
    /**
     * Gets the explicit URLs to validate.
     * 
     * @return The list of explicit URLs
     */
    public List<String> getExplicitUrls() {
        if (explicitUrls == null) {
            explicitUrls = new ArrayList<>();
        }
        return explicitUrls;
    }
    
    /**
     * Sets the explicit URLs to validate.
     * 
     * @param explicitUrls The list of explicit URLs to set
     */
    public void setExplicitUrls(List<String> explicitUrls) {
        this.explicitUrls = explicitUrls;
    }
    
    @Override
    public String toString() {
        return "ValidatedUrlCollection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", sourceMapId='" + sourceMapId + '\'' +
                ", status='" + status + '\'' +
                ", totalUrls=" + totalUrls +
                ", validUrls=" + validUrls +
                ", invalidUrls=" + invalidUrls +
                ", warningUrls=" + warningUrls +
                '}';
    }
} 