package com.spacedataarchive.validation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration properties for URL validation service.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "url.validation")
@Import(MongoConfig.class)
public class UrlValidationConfig {
    
    /**
     * User agent string to use for HTTP requests.
     */
    private String userAgent = "SpaceDataArchive-Validator/1.0";
    
    /**
     * Number of days to keep validation results before expiration.
     */
    private int resultExpirationDays = 7;
    
    /**
     * Maximum number of URLs that can be validated in a single batch request.
     */
    private int maxBatchSize = 100;
    
    /**
     * Maximum number of concurrent validation requests.
     */
    private int maxConcurrentRequests = 10;
    
    /**
     * Whether to enable caching of validation results.
     */
    private boolean cachingEnabled = true;
    
    /**
     * Whether to enable metrics collection.
     */
    private boolean metricsEnabled = true;
} 