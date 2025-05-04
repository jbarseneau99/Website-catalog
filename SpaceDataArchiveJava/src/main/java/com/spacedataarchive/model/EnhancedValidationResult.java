package com.spacedataarchive.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced validation result with additional metadata about the URL.
 * Extends the basic ValidationResult with information about the asset type and content.
 */
public class EnhancedValidationResult extends ValidationResult {
    private String urlId;              // Unique identifier from site map
    private String urlName;            // Human-readable name/title of the URL
    private String assetType;          // Type of asset (image, document, webpage, etc.)
    private String contentType;        // MIME type of the content
    private int statusCode;            // HTTP status code
    private LocalDateTime validatedAt; // When validation occurred
    private Map<String, String> metadata; // Additional metadata

    /**
     * Default constructor.
     */
    public EnhancedValidationResult() {
        super();
        this.metadata = new HashMap<>();
        this.validatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with basic validation result parameters.
     *
     * @param url The URL that was validated
     * @param status The status of the validation
     * @param message A message describing the validation result
     */
    public EnhancedValidationResult(String url, String status, String message) {
        super(url, status, message);
        this.metadata = new HashMap<>();
        this.validatedAt = LocalDateTime.now();
    }

    /**
     * Full constructor with all parameters.
     *
     * @param url The URL that was validated
     * @param status The status of the validation
     * @param message A message describing the validation result
     * @param urlId Unique identifier from site map
     * @param urlName Human-readable name/title of the URL
     * @param assetType Type of asset
     * @param contentType MIME type of the content
     * @param statusCode HTTP status code
     */
    public EnhancedValidationResult(String url, String status, String message, 
                                   String urlId, String urlName, String assetType, 
                                   String contentType, int statusCode) {
        super(url, status, message);
        this.urlId = urlId;
        this.urlName = urlName;
        this.assetType = assetType;
        this.contentType = contentType;
        this.statusCode = statusCode;
        this.metadata = new HashMap<>();
        this.validatedAt = LocalDateTime.now();
    }

    /**
     * Gets the URL ID.
     *
     * @return The URL ID
     */
    public String getUrlId() {
        return urlId;
    }

    /**
     * Sets the URL ID.
     *
     * @param urlId The URL ID to set
     */
    public void setUrlId(String urlId) {
        this.urlId = urlId;
    }

    /**
     * Gets the URL name/title.
     *
     * @return The URL name
     */
    public String getUrlName() {
        return urlName;
    }

    /**
     * Sets the URL name/title.
     *
     * @param urlName The URL name to set
     */
    public void setUrlName(String urlName) {
        this.urlName = urlName;
    }

    /**
     * Gets the asset type.
     *
     * @return The asset type
     */
    public String getAssetType() {
        return assetType;
    }

    /**
     * Sets the asset type.
     *
     * @param assetType The asset type to set
     */
    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    /**
     * Gets the content type.
     *
     * @return The content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type.
     *
     * @param contentType The content type to set
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return The HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param statusCode The HTTP status code to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets the validation timestamp.
     *
     * @return The validation timestamp
     */
    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    /**
     * Sets the validation timestamp.
     *
     * @param validatedAt The validation timestamp to set
     */
    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    /**
     * Gets the metadata.
     *
     * @return The metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata.
     *
     * @param metadata The metadata to set
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * Adds a metadata key-value pair.
     *
     * @param key The key
     * @param value The value
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return "EnhancedValidationResult{" +
                "url='" + getUrl() + '\'' +
                ", status='" + getStatus() + '\'' +
                ", message='" + getMessage() + '\'' +
                ", urlId='" + urlId + '\'' +
                ", urlName='" + urlName + '\'' +
                ", assetType='" + assetType + '\'' +
                ", contentType='" + contentType + '\'' +
                ", statusCode=" + statusCode +
                ", validatedAt=" + validatedAt +
                ", metadata=" + metadata +
                '}';
    }
} 