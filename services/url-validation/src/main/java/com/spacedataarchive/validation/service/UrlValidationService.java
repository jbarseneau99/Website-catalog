package com.spacedataarchive.validation.service;

import com.spacedataarchive.validation.model.UrlValidationRequest;
import com.spacedataarchive.validation.model.ValidatedUrl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for validating URLs.
 */
public interface UrlValidationService {
    
    /**
     * Validate a single URL with default settings.
     *
     * @param url The URL to validate
     * @param request The validation request parameters
     * @return The validation result
     */
    ValidatedUrl validateUrl(String url, UrlValidationRequest request);

    /**
     * Validate a list of URLs.
     *
     * @param urls The list of URLs to validate
     * @param request The validation request parameters
     * @return List of validation results
     */
    List<ValidatedUrl> validateUrls(List<String> urls, UrlValidationRequest request);

    /**
     * Validate a single URL asynchronously.
     *
     * @param url The URL to validate
     * @param request The validation request parameters
     * @return Future containing the validation result
     */
    default CompletableFuture<ValidatedUrl> validateUrlAsync(String url, UrlValidationRequest request) {
        return CompletableFuture.supplyAsync(() -> validateUrl(url, request));
    }

    /**
     * Find a previously validated URL result.
     *
     * @param url The URL to look up
     * @return The validation result, or null if not found
     */
    ValidatedUrl findValidatedUrl(String url);

    /**
     * Clean up expired validation results.
     *
     * @return The number of results cleaned up
     */
    long cleanupExpiredResults();

    /**
     * Shutdown the validation service gracefully.
     */
    void shutdown();
} 