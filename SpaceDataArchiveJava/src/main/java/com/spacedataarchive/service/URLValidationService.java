package com.spacedataarchive.service;

import com.spacedataarchive.model.EnhancedValidationResult;
import com.spacedataarchive.model.ValidationResult;
import com.spacedataarchive.model.ValidatedUrlCollection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for URL validation operations.
 */
public interface URLValidationService {
    
    /**
     * Constant for validation results storage.
     */
    String STEP_VALIDATION = "validation";
    
    /**
     * Validates a single URL.
     * 
     * @param url The URL to validate
     * @return A ValidationResult containing the validation details
     */
    ValidationResult validateUrl(String url);
    
    /**
     * Validates a URL with additional AI-assisted analysis.
     * 
     * @param url The URL to validate
     * @return An EnhancedValidationResult with additional metadata
     */
    EnhancedValidationResult validateUrlWithAI(String url);
    
    /**
     * Validates a batch of URLs.
     * 
     * @param urls A list of URLs to validate
     * @return A list of validation results
     */
    List<ValidationResult> validateUrls(List<String> urls);
    
    /**
     * Creates a new validation collection from a site map.
     * 
     * @param siteMapId The ID of the site map
     * @param name The name of the validation collection
     * @return The created validation collection
     */
    ValidatedUrlCollection createValidationFromSiteMap(String siteMapId, String name);
    
    /**
     * Creates a new validation collection with an explicit list of URLs.
     * 
     * @param siteMapId The ID of the associated site map
     * @param name The name of the validation collection
     * @param urls The list of URLs to validate
     * @return The created validation collection
     */
    ValidatedUrlCollection createValidationWithUrls(String siteMapId, String name, List<String> urls);
    
    /**
     * Retrieves a validation collection.
     * 
     * @param validationId The ID of the validation collection
     * @return The validation collection
     */
    ValidatedUrlCollection getValidationCollection(String validationId);
    
    /**
     * Lists all validation collections.
     * 
     * @return A list of validation collections
     */
    List<ValidatedUrlCollection> listValidationCollections();
    
    /**
     * Starts validating URLs from a site map asynchronously.
     * 
     * @param validationId The ID of the validation collection
     * @param concurrentRequests The number of concurrent validation requests
     * @param useAI Whether to use AI for enhanced validation
     * @return A CompletableFuture that completes when validation is done
     */
    CompletableFuture<ValidatedUrlCollection> startValidation(String validationId, int concurrentRequests, boolean useAI);
    
    /**
     * Gets the status of a validation process.
     * 
     * @param validationId The ID of the validation collection
     * @return A map of status information
     */
    Map<String, Object> getValidationStatus(String validationId);
    
    /**
     * Stops an ongoing validation process.
     * 
     * @param validationId The ID of the validation collection
     * @return True if the validation was stopped successfully
     */
    boolean stopValidation(String validationId);
    
    /**
     * Detects the asset type from a URL and content type.
     * 
     * @param url The URL
     * @param contentType The content type (MIME type)
     * @return The detected asset type
     */
    String detectAssetType(String url, String contentType);
    
    /**
     * Extracts a human-readable name from a URL.
     * 
     * @param url The URL
     * @return A human-readable name
     */
    String extractUrlName(String url);
} 