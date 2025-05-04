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
 * Represents the results of a catalog processing operation.
 * This can be for any of the processing phases: mapping, validation, extraction, or enhancement.
 */
public class CatalogProcessingResult {
    
    private final String id;
    private final String jobId;
    private final String phase;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private boolean completed = false;
    private boolean successful = false;
    private String errorMessage;
    
    // URLs processed in this operation
    private final List<ProcessedUrl> processedUrls = new ArrayList<>();
    
    // Metrics and statistics
    private final Map<String, Object> metrics = new HashMap<>();
    
    // Success and error counts for direct manipulation
    private int successCount = 0;
    private int errorCount = 0;
    
    /**
     * Default constructor for JSON deserialization.
     */
    public CatalogProcessingResult() {
        this.id = UUID.randomUUID().toString();
        this.jobId = "";
        this.phase = "";
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Creates a new processing result.
     *
     * @param jobId The ID of the processing job
     * @param phase The processing phase
     */
    public CatalogProcessingResult(String jobId, String phase) {
        this.id = UUID.randomUUID().toString();
        this.jobId = jobId;
        this.phase = phase;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Creates a new processing result with all parameters.
     * Used for JSON deserialization.
     */
    @JsonCreator
    public CatalogProcessingResult(
            @JsonProperty("id") String id,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("phase") String phase,
            @JsonProperty("createdAt") LocalDateTime createdAt,
            @JsonProperty("completedAt") LocalDateTime completedAt,
            @JsonProperty("completed") boolean completed,
            @JsonProperty("successful") boolean successful,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("processedUrls") List<ProcessedUrl> processedUrls,
            @JsonProperty("metrics") Map<String, Object> metrics) {
        this.id = id;
        this.jobId = jobId;
        this.phase = phase;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.completed = completed;
        this.successful = successful;
        this.errorMessage = errorMessage;
        
        if (processedUrls != null) {
            this.processedUrls.addAll(processedUrls);
        }
        
        if (metrics != null) {
            this.metrics.putAll(metrics);
        }
    }
    
    /**
     * Represents a URL that has been processed.
     */
    public static class ProcessedUrl {
        private final String url;
        private final String status;
        private final String message;
        private final Map<String, Object> metadata = new HashMap<>();
        
        /**
         * Default constructor for JSON deserialization.
         */
        public ProcessedUrl() {
            this.url = "";
            this.status = "";
            this.message = "";
        }
        
        /**
         * Creates a new processed URL entry.
         *
         * @param url The URL
         * @param status The processing status
         * @param message A message about the processing
         */
        public ProcessedUrl(String url, String status, String message) {
            this.url = url;
            this.status = status;
            this.message = message;
        }
        
        /**
         * Creates a new processed URL entry with all parameters.
         * Used for JSON deserialization.
         */
        @JsonCreator
        public ProcessedUrl(
                @JsonProperty("url") String url,
                @JsonProperty("status") String status,
                @JsonProperty("message") String message,
                @JsonProperty("metadata") Map<String, Object> metadata) {
            this.url = url;
            this.status = status;
            this.message = message;
            
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
        }
        
        public String getUrl() {
            return url;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void addMetadata(String key, Object value) {
            this.metadata.put(key, value);
        }
    }
    
    public String getId() {
        return id;
    }
    
    public String getJobId() {
        return jobId;
    }
    
    public String getPhase() {
        return phase;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public List<ProcessedUrl> getProcessedUrls() {
        return processedUrls;
    }
    
    public void addProcessedUrl(ProcessedUrl processedUrl) {
        this.processedUrls.add(processedUrl);
    }
    
    public void addProcessedUrl(String url, String status, String message) {
        this.processedUrls.add(new ProcessedUrl(url, status, message));
    }
    
    public Map<String, Object> getMetrics() {
        return metrics;
    }
    
    public void addMetric(String key, Object value) {
        this.metrics.put(key, value);
    }
    
    /**
     * Gets the total number of processed URLs.
     *
     * @return The total count
     */
    public int getTotalProcessedCount() {
        return processedUrls.size();
    }
    
    /**
     * Gets the number of successfully processed URLs.
     *
     * @return The success count
     */
    public int getSuccessCount() {
        // If success count is explicitly set, use that value
        if (successCount > 0) {
            return successCount;
        }
        
        // Otherwise calculate from processed URLs
        return (int) processedUrls.stream()
                .filter(url -> "success".equalsIgnoreCase(url.getStatus()))
                .count();
    }
    
    /**
     * Sets the success count directly.
     * 
     * @param successCount The success count to set
     */
    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
        this.metrics.put("successCount", successCount);
    }
    
    /**
     * Gets the number of URLs with warnings.
     *
     * @return The warning count
     */
    public int getWarningCount() {
        return (int) processedUrls.stream()
                .filter(url -> "warning".equalsIgnoreCase(url.getStatus()))
                .count();
    }
    
    /**
     * Gets the number of URLs with errors.
     *
     * @return The error count
     */
    public int getErrorCount() {
        // If error count is explicitly set, use that value
        if (errorCount > 0) {
            return errorCount;
        }
        
        // Otherwise calculate from processed URLs
        return (int) processedUrls.stream()
                .filter(url -> "error".equalsIgnoreCase(url.getStatus()))
                .count();
    }
    
    /**
     * Sets the error count directly.
     * 
     * @param errorCount The error count to set
     */
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
        this.metrics.put("errorCount", errorCount);
    }
    
    /**
     * Marks the processing as complete and successful.
     */
    public void markAsSuccessful() {
        this.completed = true;
        this.successful = true;
        this.completedAt = LocalDateTime.now();
    }
    
    /**
     * Marks the processing as complete with an error.
     *
     * @param errorMessage The error message
     */
    public void markAsFailed(String errorMessage) {
        this.completed = true;
        this.successful = false;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }
} 