package com.spacedataarchive.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a catalog processing job that will go through mapping, validation,
 * extraction, and enhancement phases.
 */
public class ProcessingJob {
    
    private final String id;
    private final String name;
    private final String seedUrl;
    private final int maxDepth;
    private final int maxUrls;
    private final LocalDateTime createdAt;
    private LocalDateTime lastModified;
    
    // Status of each processing phase
    private ProcessingPhaseStatus mappingStatus = ProcessingPhaseStatus.NOT_STARTED;
    private ProcessingPhaseStatus validationStatus = ProcessingPhaseStatus.NOT_STARTED;
    private ProcessingPhaseStatus extractionStatus = ProcessingPhaseStatus.NOT_STARTED;
    private ProcessingPhaseStatus enhancementStatus = ProcessingPhaseStatus.NOT_STARTED;
    
    // IDs for results from each phase
    private String mappingResultId;
    private String validationResultId;
    private String extractionResultId;
    private String enhancementResultId;
    
    // Statistics for each phase
    private final Map<String, Object> statistics = new HashMap<>();
    
    // Configuration properties
    private final Map<String, Object> configuration = new HashMap<>();
    
    /**
     * Default constructor for JSON deserialization.
     */
    public ProcessingJob() {
        this.id = UUID.randomUUID().toString();
        this.name = "";
        this.seedUrl = "";
        this.maxDepth = 0;
        this.maxUrls = 0;
        this.createdAt = LocalDateTime.now();
        this.lastModified = this.createdAt;
    }
    
    /**
     * Creates a new processing job.
     *
     * @param name The job name
     * @param seedUrl The seed URL
     * @param maxDepth The maximum crawl depth
     * @param maxUrls The maximum number of URLs to process
     */
    public ProcessingJob(String name, String seedUrl, int maxDepth, int maxUrls) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.seedUrl = seedUrl;
        this.maxDepth = maxDepth;
        this.maxUrls = maxUrls;
        this.createdAt = LocalDateTime.now();
        this.lastModified = this.createdAt;
        
        // Set default configuration options
        this.configuration.put("validateDuringMapping", true);
        this.configuration.put("concurrentValidations", 10);
        this.configuration.put("extractMetadata", true);
        this.configuration.put("enhanceWithAI", false);
    }
    
    /**
     * Creates a new processing job with all parameters.
     * Used for JSON deserialization.
     */
    @JsonCreator
    public ProcessingJob(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("seedUrl") String seedUrl,
            @JsonProperty("maxDepth") int maxDepth,
            @JsonProperty("maxUrls") int maxUrls,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("lastModified") LocalDateTime lastModified,
            @JsonProperty("mappingStatus") ProcessingPhaseStatus mappingStatus,
            @JsonProperty("validationStatus") ProcessingPhaseStatus validationStatus,
            @JsonProperty("extractionStatus") ProcessingPhaseStatus extractionStatus,
            @JsonProperty("enhancementStatus") ProcessingPhaseStatus enhancementStatus,
            @JsonProperty("mappingResultId") String mappingResultId,
            @JsonProperty("validationResultId") String validationResultId,
            @JsonProperty("extractionResultId") String extractionResultId,
            @JsonProperty("enhancementResultId") String enhancementResultId,
            @JsonProperty("statistics") Map<String, Object> statistics,
            @JsonProperty("configuration") Map<String, Object> configuration) {
        this.id = id;
        this.name = name;
        this.seedUrl = seedUrl;
        this.maxDepth = maxDepth;
        this.maxUrls = maxUrls;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.mappingStatus = mappingStatus;
        this.validationStatus = validationStatus;
        this.extractionStatus = extractionStatus;
        this.enhancementStatus = enhancementStatus;
        this.mappingResultId = mappingResultId;
        this.validationResultId = validationResultId;
        this.extractionResultId = extractionResultId;
        this.enhancementResultId = enhancementResultId;
        
        if (statistics != null) {
            this.statistics.putAll(statistics);
        }
        
        if (configuration != null) {
            this.configuration.putAll(configuration);
        } else {
            // Set default configuration options
            this.configuration.put("validateDuringMapping", true);
            this.configuration.put("concurrentValidations", 10);
            this.configuration.put("extractMetadata", true);
            this.configuration.put("enhanceWithAI", false);
        }
    }
    
    /**
     * Status of a processing phase.
     */
    public enum ProcessingPhaseStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getSeedUrl() {
        return seedUrl;
    }
    
    public int getMaxDepth() {
        return maxDepth;
    }
    
    public int getMaxUrls() {
        return maxUrls;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    public ProcessingPhaseStatus getMappingStatus() {
        return mappingStatus;
    }
    
    public void setMappingStatus(ProcessingPhaseStatus mappingStatus) {
        this.mappingStatus = mappingStatus;
        this.lastModified = LocalDateTime.now();
    }
    
    public ProcessingPhaseStatus getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(ProcessingPhaseStatus validationStatus) {
        this.validationStatus = validationStatus;
        this.lastModified = LocalDateTime.now();
    }
    
    public ProcessingPhaseStatus getExtractionStatus() {
        return extractionStatus;
    }
    
    public void setExtractionStatus(ProcessingPhaseStatus extractionStatus) {
        this.extractionStatus = extractionStatus;
        this.lastModified = LocalDateTime.now();
    }
    
    public ProcessingPhaseStatus getEnhancementStatus() {
        return enhancementStatus;
    }
    
    public void setEnhancementStatus(ProcessingPhaseStatus enhancementStatus) {
        this.enhancementStatus = enhancementStatus;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getMappingResultId() {
        return mappingResultId;
    }
    
    public void setMappingResultId(String mappingResultId) {
        this.mappingResultId = mappingResultId;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getValidationResultId() {
        return validationResultId;
    }
    
    public void setValidationResultId(String validationResultId) {
        this.validationResultId = validationResultId;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getExtractionResultId() {
        return extractionResultId;
    }
    
    public void setExtractionResultId(String extractionResultId) {
        this.extractionResultId = extractionResultId;
        this.lastModified = LocalDateTime.now();
    }
    
    public String getEnhancementResultId() {
        return enhancementResultId;
    }
    
    public void setEnhancementResultId(String enhancementResultId) {
        this.enhancementResultId = enhancementResultId;
        this.lastModified = LocalDateTime.now();
    }
    
    public Map<String, Object> getStatistics() {
        return statistics;
    }
    
    public void addStatistic(String key, Object value) {
        this.statistics.put(key, value);
        this.lastModified = LocalDateTime.now();
    }
    
    public Map<String, Object> getConfiguration() {
        return configuration;
    }
    
    public void setConfiguration(String key, Object value) {
        this.configuration.put(key, value);
        this.lastModified = LocalDateTime.now();
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getConfiguration(String key, T defaultValue) {
        return (T) configuration.getOrDefault(key, defaultValue);
    }
} 