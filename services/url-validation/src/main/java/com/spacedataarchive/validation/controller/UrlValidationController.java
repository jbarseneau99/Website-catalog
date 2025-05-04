package com.spacedataarchive.validation.controller;

import com.spacedataarchive.validation.config.UrlValidationConfig;
import com.spacedataarchive.validation.model.UrlValidationRequest;
import com.spacedataarchive.validation.model.ValidatedUrl;
import com.spacedataarchive.validation.service.UrlValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for URL validation endpoints.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/validation")
public class UrlValidationController {
    
    private final UrlValidationService validationService;
    private final UrlValidationConfig config;
    
    /**
     * Validate a single URL with default settings.
     *
     * @param url The URL to validate
     * @return The validation result
     */
    @GetMapping("/url")
    public ResponseEntity<ValidatedUrl> validateUrl(@RequestParam @NotEmpty String url) {
        log.info("Received validation request for URL: {}", url);
        
        UrlValidationRequest request = UrlValidationRequest.builder()
                .url(url)
                .build();
        
        try {
            ValidatedUrl result = validationService.validateUrl(url, request);
            log.info("Validation completed for URL: {}", url);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating URL {}: {}", url, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Validate a single URL with custom settings.
     *
     * @param request The validation request
     * @return The validation result
     */
    @PostMapping("/url")
    public ResponseEntity<ValidatedUrl> validateUrlWithSettings(@RequestBody @Valid UrlValidationRequest request) {
        log.info("Received validation request with custom settings for URL: {}", request.getUrl());
        
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            ValidatedUrl result = validationService.validateUrl(request.getUrl(), request);
            log.info("Validation completed for URL: {}", request.getUrl());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error validating URL: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Validate a URL asynchronously.
     *
     * @param url The URL to validate
     * @return A future containing the validation result
     */
    @GetMapping("/url/async")
    public CompletableFuture<ResponseEntity<ValidatedUrl>> validateUrlAsync(@RequestParam @NotEmpty String url) {
        log.info("Received async validation request for URL: {}", url);
        
        UrlValidationRequest request = UrlValidationRequest.builder()
                .url(url)
                .build();
        
        return validationService.validateUrlAsync(url, request)
                .thenApply(result -> {
                    log.info("Async validation completed for URL: {}, valid: {}", url, result.isValid());
                    return ResponseEntity.ok(result);
                })
                .exceptionally(e -> {
                    log.error("Error in async validation for URL {}: {}", url, e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }
    
    /**
     * Validate multiple URLs in batch.
     *
     * @param request The batch validation request
     * @return List of validation results
     */
    @PostMapping("/urls")
    public ResponseEntity<List<ValidatedUrl>> validateUrls(@RequestBody @Valid UrlValidationRequest request) {
        log.info("Received batch validation request for {} URLs", request.getUrls().size());
        
        if (request.getUrls() == null || request.getUrls().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (request.getUrls().size() > config.getMaxBatchSize()) {
            log.warn("Batch size {} exceeds maximum allowed {}", request.getUrls().size(), config.getMaxBatchSize());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<ValidatedUrl> results = validationService.validateUrls(request.getUrls(), request);
            log.info("Batch validation completed for {} URLs", request.getUrls().size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error in batch validation: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get a previously validated URL result.
     *
     * @param url The URL to look up
     * @return The validation result if found
     */
    @GetMapping("/url/result")
    public ResponseEntity<ValidatedUrl> getValidationResult(@RequestParam @NotEmpty String url) {
        log.info("Looking up validation result for URL: {}", url);
        
        ValidatedUrl result = validationService.findValidatedUrl(url);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (result.isExpired()) {
            log.info("Found expired validation result for URL: {}", url);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Clean up expired validation results.
     *
     * @return The number of results deleted
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Long> cleanupExpiredResults() {
        log.info("Starting cleanup of expired validation results");
        long deletedCount = validationService.cleanupExpiredResults();
        log.info("Cleaned up {} expired validation results", deletedCount);
        return ResponseEntity.ok(deletedCount);
    }
} 