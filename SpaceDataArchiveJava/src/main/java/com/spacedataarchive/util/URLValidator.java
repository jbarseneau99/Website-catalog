package com.spacedataarchive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Utility class for fast URL validation during crawling.
 * This is designed for high-performance validation with minimal overhead.
 */
public class URLValidator {
    private static final Logger logger = LoggerFactory.getLogger(URLValidator.class);
    
    // Cache validation results to avoid repeated validations
    private static final ConcurrentHashMap<String, CachedValidationResult> validationCache = new ConcurrentHashMap<>();
    
    // Timeout for connection in milliseconds
    private static final int CONNECTION_TIMEOUT = 5000;
    
    // Maximum content size to check in bytes (5MB)
    private static final int MAX_CONTENT_SIZE = 5 * 1024 * 1024;
    
    // Valid content types for assets
    private static final Set<String> VALID_CONTENT_TYPES = new HashSet<>(Arrays.asList(
        "text/html", 
        "application/pdf", 
        "image/jpeg", 
        "image/png", 
        "image/gif",
        "application/json",
        "text/plain",
        "application/xml",
        "text/xml",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "image/tiff",
        "image/svg+xml",
        "application/zip",
        "application/x-hdf",
        "application/x-netcdf",
        "application/x-hdf5",
        "application/x-fits",
        "application/fits",
        "application/octet-stream",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "audio/mpeg",
        "video/mp4",
        "application/geo+json"
    ));
    
    // Pattern for valid URL syntax
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)"  // Protocol
        + "([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}" // Domain name
        + "(:[0-9]{1,5})?" // Port (optional)
        + "(/[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?" // Path (optional)
    );
    
    // Default cache expiration time in minutes
    private static final int DEFAULT_CACHE_EXPIRATION_MINUTES = 60;
    
    /**
     * Result of a URL validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final String contentType;
        private final long contentLength;
        
        public ValidationResult(boolean valid, String reason, String contentType, long contentLength) {
            this.valid = valid;
            this.reason = reason;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getContentType() {
            return contentType;
        }
        
        public long getContentLength() {
            return contentLength;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                   "valid=" + valid +
                   ", reason='" + reason + '\'' +
                   ", contentType='" + contentType + '\'' +
                   ", contentLength=" + contentLength +
                   '}';
        }
    }
    
    /**
     * Cached validation result with timestamp.
     */
    private static class CachedValidationResult {
        private final ValidationResult result;
        private final long timestamp;
        
        public CachedValidationResult(ValidationResult result) {
            this.result = result;
            this.timestamp = Instant.now().getEpochSecond();
        }
        
        public ValidationResult getResult() {
            return result;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isExpired(int maxAgeMinutes) {
            long expirationTime = timestamp + (maxAgeMinutes * 60);
            return Instant.now().getEpochSecond() > expirationTime;
        }
    }
    
    /**
     * Fast syntax validation for a URL without making a network request.
     * 
     * @param urlString The URL to validate
     * @return true if the URL syntax is valid
     */
    public static boolean hasValidSyntax(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return false;
        }
        
        // Check URL syntax
        return URL_PATTERN.matcher(urlString).matches();
    }
    
    /**
     * Full validation of a URL including HTTP request to check availability.
     * 
     * @param urlString The URL to validate
     * @return ValidationResult containing validation details
     */
    public static ValidationResult validateUrl(String urlString) {
        return validateUrl(urlString, DEFAULT_CACHE_EXPIRATION_MINUTES);
    }
    
    /**
     * Full validation of a URL including HTTP request to check availability.
     * 
     * @param urlString The URL to validate
     * @param cacheExpirationMinutes Minutes before cache entry expires
     * @return ValidationResult containing validation details
     */
    public static ValidationResult validateUrl(String urlString, int cacheExpirationMinutes) {
        // First check cache
        if (validationCache.containsKey(urlString)) {
            CachedValidationResult cachedResult = validationCache.get(urlString);
            if (!cachedResult.isExpired(cacheExpirationMinutes)) {
                return cachedResult.getResult();
            } else {
                // Remove expired result
                validationCache.remove(urlString);
            }
        }
        
        // First check syntax
        if (!hasValidSyntax(urlString)) {
            ValidationResult result = new ValidationResult(false, "Invalid URL syntax", null, 0);
            validationCache.put(urlString, new CachedValidationResult(result));
            return result;
        }
        
        HttpURLConnection connection = null;
        try {
            // Open connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(CONNECTION_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            
            // Add a user agent to avoid being blocked
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36");
            
            // Get response code
            int responseCode = connection.getResponseCode();
            
            // Get content type and length
            String contentType = connection.getContentType();
            long contentLength = connection.getContentLengthLong();
            
            // Check response code
            if (responseCode != HttpURLConnection.HTTP_OK) {
                ValidationResult result = new ValidationResult(
                    false, 
                    "Invalid response code: " + responseCode,
                    contentType,
                    contentLength
                );
                validationCache.put(urlString, new CachedValidationResult(result));
                return result;
            }
            
            // Check content type
            if (contentType == null || !isValidContentType(contentType)) {
                ValidationResult result = new ValidationResult(
                    false, 
                    "Invalid content type: " + contentType,
                    contentType,
                    contentLength
                );
                validationCache.put(urlString, new CachedValidationResult(result));
                return result;
            }
            
            // Check content length
            if (contentLength > MAX_CONTENT_SIZE) {
                ValidationResult result = new ValidationResult(
                    false, 
                    "Content too large: " + contentLength + " bytes",
                    contentType,
                    contentLength
                );
                validationCache.put(urlString, new CachedValidationResult(result));
                return result;
            }
            
            // If we get here, the URL is valid
            ValidationResult result = new ValidationResult(
                true, 
                "Valid URL",
                contentType,
                contentLength
            );
            validationCache.put(urlString, new CachedValidationResult(result));
            return result;
            
        } catch (IOException e) {
            ValidationResult result = new ValidationResult(
                false, 
                "Connection error: " + e.getMessage(),
                null,
                0
            );
            validationCache.put(urlString, new CachedValidationResult(result));
            return result;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Check if content type is valid.
     * 
     * @param contentType The content type to check
     * @return true if the content type is valid
     */
    private static boolean isValidContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Extract main content type
        String mainType = contentType.split(";")[0].trim().toLowerCase();
        
        return VALID_CONTENT_TYPES.contains(mainType);
    }
    
    /**
     * Clears the validation cache.
     */
    public static void clearCache() {
        validationCache.clear();
    }
    
    /**
     * Removes expired entries from cache.
     * 
     * @param maxAgeMinutes Maximum age in minutes for cache entries
     * @return Number of entries removed
     */
    public static int cleanupCache(int maxAgeMinutes) {
        int removedCount = 0;
        for (Map.Entry<String, CachedValidationResult> entry : validationCache.entrySet()) {
            if (entry.getValue().isExpired(maxAgeMinutes)) {
                validationCache.remove(entry.getKey());
                removedCount++;
            }
        }
        logger.debug("Cleaned up {} expired cache entries", removedCount);
        return removedCount;
    }
    
    /**
     * Get cache statistics.
     * 
     * @return Map containing cache statistics
     */
    public static Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("size", validationCache.size());
        
        int expiredCount = 0;
        for (CachedValidationResult result : validationCache.values()) {
            if (result.isExpired(DEFAULT_CACHE_EXPIRATION_MINUTES)) {
                expiredCount++;
            }
        }
        
        stats.put("expiredEntries", expiredCount);
        
        return stats;
    }
} 