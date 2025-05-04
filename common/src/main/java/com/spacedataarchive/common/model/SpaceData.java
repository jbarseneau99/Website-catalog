package com.spacedataarchive.common.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "space_data")
public class SpaceData {
    @Id
    private String id;

    @NotBlank
    private String title;
    
    @NotBlank
    private String url;
    
    private String description;
    private List<String> keywords;
    private Map<String, String> metadata;
    private String contentType;
    private LocalDateTime discoveredAt;
    private LocalDateTime lastProcessedAt;
    private ProcessingStatus status;
    
    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public LocalDateTime getLastProcessedAt() {
        return lastProcessedAt;
    }

    public void setLastProcessedAt(LocalDateTime lastProcessedAt) {
        this.lastProcessedAt = lastProcessedAt;
    }

    public ProcessingStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingStatus status) {
        this.status = status;
    }
} 