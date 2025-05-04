package com.spacedataarchive.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents space-related data with metadata and validation status.
 * This class is immutable and thread-safe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class SpaceData {
    
    /**
     * Unique identifier for the space data.
     */
    private String id;
    
    /**
     * Title or name of the space data.
     */
    private String title;
    
    /**
     * URL where the space data was found.
     */
    private String url;
    
    /**
     * Description of the space data.
     */
    private String description;
    
    /**
     * List of keywords associated with the space data.
     */
    private List<String> keywords;
    
    /**
     * Additional metadata as key-value pairs.
     */
    private Map<String, Object> metadata;
    
    /**
     * Content type of the space data.
     */
    private String contentType;
    
    /**
     * When the space data was discovered.
     */
    private LocalDateTime discoveredAt;
    
    /**
     * When the space data was last processed.
     */
    private LocalDateTime lastProcessedAt;
    
    /**
     * Current validation status.
     */
    private ValidationStatus status;
}