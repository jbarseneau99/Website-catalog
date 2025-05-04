package com.spacedataarchive.validation.service;

import com.spacedataarchive.common.model.ValidationStatus;
import com.spacedataarchive.validation.config.UrlValidationConfig;
import com.spacedataarchive.validation.model.UrlValidationRequest;
import com.spacedataarchive.validation.model.ValidatedUrl;
import com.spacedataarchive.validation.repository.ValidatedUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of the URL validation service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlValidationServiceImpl implements UrlValidationService {
    
    private final UrlValidationConfig config;
    private final ValidatedUrlRepository repository;
    
    @Override
    public ValidatedUrl validateUrl(String url, UrlValidationRequest validationRequest) {
        log.info("Starting validation for URL: {}", url);
        
        ValidatedUrl result = ValidatedUrl.builder()
                .url(url)
                .validatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(config.getResultExpirationDays()))
                .status(ValidationStatus.IN_PROGRESS)
                .build();

        try {
            log.debug("Creating HTTP client with timeout settings: connect={}ms, socket={}ms",
                    validationRequest.getConnectTimeoutMs(), validationRequest.getSocketTimeoutMs());

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(validationRequest.getConnectTimeoutMs()))
                    .setResponseTimeout(Timeout.ofMilliseconds(validationRequest.getSocketTimeoutMs()))
                    .setRedirectsEnabled(validationRequest.isFollowRedirects())
                    .setMaxRedirects(validationRequest.getMaxRedirects())
                    .build();

            CloseableHttpClient httpClient = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .build();

            HttpGet request = new HttpGet(url);
            request.setHeader(HttpHeaders.USER_AGENT, config.getUserAgent());

            long startTime = System.currentTimeMillis();

            httpClient.execute(request, response -> {
                result.setResponseTimeMs(System.currentTimeMillis() - startTime);
                result.setStatusCode(response.getCode());
                result.setValid(response.getCode() >= 200 && response.getCode() < 400);

                // Check content type if requested
                if (validationRequest.isValidateContentType()) {
                    Header contentTypeHeader = response.getHeader(HttpHeaders.CONTENT_TYPE);
                    if (contentTypeHeader != null) {
                        result.setContentType(contentTypeHeader.getValue());
                    }
                }

                // Get content length if available
                Header contentLengthHeader = response.getHeader(HttpHeaders.CONTENT_LENGTH);
                if (contentLengthHeader != null) {
                    result.setContentLengthBytes(Long.parseLong(contentLengthHeader.getValue()));
                    log.debug("Content length for {}: {} bytes", url, result.getContentLengthBytes());
                }

                // Handle redirects
                if (response.getCode() >= 300 && response.getCode() < 400) {
                    result.setRedirect(true);
                    result.setRedirectUrl(response.getHeader(HttpHeaders.LOCATION).getValue());
                }

                result.setStatus(ValidationStatus.SUCCESS);
                log.info("URL validation successful: {}", url);

                return result;
            });

            return repository.save(result);

        } catch (IOException e) {
            log.error("Error validating URL {}: {}", url, e.getMessage());
            result.setValid(false);
            result.setError(e.getMessage());
            result.setStatus(ValidationStatus.ERROR);
            return repository.save(result);
        }
    }
    
    @Override
    public List<ValidatedUrl> validateUrls(List<String> urls, UrlValidationRequest request) {
        log.info("Starting batch validation for {} URLs", urls.size());

        if (request.isParallel()) {
            return urls.parallelStream()
                    .map(url -> validateUrl(url, request))
                    .collect(Collectors.toList());
        }

        return urls.stream()
                .map(url -> validateUrl(url, request))
                .collect(Collectors.toList());
    }
    
    @Override
    public CompletableFuture<ValidatedUrl> validateUrlAsync(String url, UrlValidationRequest request) {
        log.info("Starting async validation for URL: {}", url);

        try {
            return repository.findByUrl(url)
                    .filter(result -> !result.isExpired())
                    .map(result -> {
                        log.info("Found valid cached result for URL: {}", url);
                        return CompletableFuture.completedFuture(result);
                    })
                    .orElseGet(() -> CompletableFuture.supplyAsync(() -> validateUrl(url, request)));
        } catch (Exception e) {
            log.warn("Error checking cached result for URL {}: {}", url, e.getMessage());
            return CompletableFuture.supplyAsync(() -> validateUrl(url, request));
        }
    }
    
    @Override
    public ValidatedUrl findValidatedUrl(String url) {
        log.debug("Looking up validation result for URL: {}", url);
        return repository.findByUrl(url).orElse(null);
    }
    
    @Override
    public long cleanupExpiredResults() {
        log.info("Starting cleanup of expired validation results");
        return repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
    
    @Override
    public void shutdown() {
        log.info("Shutting down URL validation service");
    }
}