package com.spacedataarchive.validation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * Request model for URL validation operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlValidationRequest {
    
    /**
     * Single URL to validate.
     */
    @NotEmpty(message = "URL cannot be empty")
    private String url;
    
    /**
     * List of URLs to validate in batch.
     */
    @Size(max = 100, message = "Maximum 100 URLs allowed")
    private List<@NotEmpty String> urls;
    
    /**
     * Connection timeout in milliseconds.
     */
    @Builder.Default
    @Min(value = 1000, message = "Connection timeout must be at least 1000ms")
    @Max(value = 30000, message = "Connection timeout cannot exceed 30000ms")
    private int connectTimeoutMs = 5000;
    
    /**
     * Socket timeout in milliseconds.
     */
    @Builder.Default
    @Min(value = 1000, message = "Socket timeout must be at least 1000ms")
    @Max(value = 30000, message = "Socket timeout cannot exceed 30000ms")
    private int socketTimeoutMs = 10000;
    
    /**
     * Whether to follow redirects.
     */
    @Builder.Default
    private boolean followRedirects = true;
    
    /**
     * Maximum number of redirects to follow.
     */
    @Builder.Default
    @Min(value = 0, message = "Max redirects cannot be negative")
    @Max(value = 10, message = "Max redirects cannot exceed 10")
    private int maxRedirects = 5;
    
    /**
     * Whether to validate content type.
     */
    @Builder.Default
    private boolean validateContentType = true;
    
    /**
     * Whether to process URLs in parallel.
     */
    @Builder.Default
    private boolean parallel = false;
} 