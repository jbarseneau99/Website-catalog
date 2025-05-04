package com.spacedataarchive.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the result of validating a URL.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlValidationResult {
    
    private String url;
    private boolean valid;
    private int statusCode;
    private String contentType;
    private long responseTimeMs;
    private LocalDateTime validatedAt;
    private String error;
    private long contentLengthBytes;
    private boolean redirect;
    private String redirectUrl;
    
    @Builder.Default
    private ValidationStatus status = ValidationStatus.PENDING;
    
    /**
     * Validation status for a URL.
     */
    public enum ValidationStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        TIMEOUT
    }
} 