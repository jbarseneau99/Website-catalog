package com.spacedataarchive.model;

/**
 * Represents a result from a URL validation operation.
 */
public class ValidationResult {
    private String url;
    private String status;
    private String message;
    private String contentType;
    private boolean valid;
    
    /**
     * Default constructor.
     */
    public ValidationResult() {
    }
    
    /**
     * Constructor with parameters.
     * 
     * @param url The URL that was validated
     * @param status The status of the validation
     * @param message A message describing the validation result
     */
    public ValidationResult(String url, String status, String message) {
        this.url = url;
        this.status = status;
        this.message = message;
        this.valid = "OK".equals(status) || "200".equals(status);
    }
    
    /**
     * Constructor with parameters including content type.
     * 
     * @param url The URL that was validated
     * @param status The status of the validation
     * @param message A message describing the validation result
     * @param contentType The content type of the URL resource
     * @param valid Whether the URL is valid
     */
    public ValidationResult(String url, String status, String message, String contentType, boolean valid) {
        this.url = url;
        this.status = status;
        this.message = message;
        this.contentType = contentType;
        this.valid = valid;
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
     * Determines if the URL is valid.
     * 
     * @return true if the URL is valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Sets whether the URL is valid.
     * 
     * @param valid Whether the URL is valid
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "url='" + url + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", contentType='" + contentType + '\'' +
                ", valid=" + valid +
                '}';
    }
} 