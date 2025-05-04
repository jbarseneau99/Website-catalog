package com.spacedataarchive.service;

import com.spacedataarchive.model.EnhancedValidationResult;
import com.spacedataarchive.model.ValidationResult;
import com.spacedataarchive.model.ValidatedUrlCollection;
import com.spacedataarchive.service.ai.AIMemoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Implementation of the URL validation service.
 */
public class URLValidationServiceImpl implements URLValidationService {
    private static final Logger logger = LoggerFactory.getLogger(URLValidationServiceImpl.class);
    private static final String AI_FUNCTION_ID = "url-validation";
    private static final String STEP_VALIDATION = "validation";
    
    private final ExecutorService executorService;
    private final StorageService storageService;
    private final AIMemoryService aiMemoryService;
    
    // Track active validation processes
    private final Map<String, CompletableFuture<ValidatedUrlCollection>> activeValidations = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> validationStatus = new ConcurrentHashMap<>();
    
    // Flag to indicate service is shutting down
    private volatile boolean shutdownFlag = false;
    
    // Timeout settings
    private static final int CONNECTION_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 15000; // 15 seconds
    
    /**
     * Constructor with dependencies.
     * 
     * @param siteMapService The site map service
     * @param aiMemoryService The AI memory service
     */
    public URLValidationServiceImpl(SiteMapService siteMapService, AIMemoryService aiMemoryService) {
        this.executorService = Executors.newFixedThreadPool(10);
        this.storageService = StorageService.getInstance();
        this.aiMemoryService = aiMemoryService;
        
        // Register the validation function with the AI memory service
        if (aiMemoryService != null) {
            aiMemoryService.registerFunction(
                AI_FUNCTION_ID, 
                "URL Validation", 
                "Validates URLs for security, content relevance, and technical compliance",
                AIMemoryService.AIFunctionType.FUNCTION
            );
        }
        
        logger.info("URLValidationService initialized");
    }
    
    @Override
    public ValidationResult validateUrl(String url) {
        // Check if service is shutting down
        if (shutdownFlag) {
            logger.debug("Skipping URL validation as service is shutting down: {}", url);
            return new ValidationResult(url, "Canceled", "Validation service is shutting down", "unknown/unknown", false);
        }
        
        logger.debug("Validating URL: {}", url);
        
        if (url == null || url.trim().isEmpty()) {
            return new ValidationResult("", "Error", "URL cannot be empty");
        }
        
        try {
            // Validate URL format by creating a URI object
            URI uri = new URI(url);
            
            // Check if the URI has a scheme
            if (uri.getScheme() == null) {
                return new ValidationResult(url, "Error", "URL missing scheme (http:// or https://)", null, false);
            }
            
            // For security, only allow HTTP and HTTPS URLs
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                return new ValidationResult(url, "Error", "Only HTTP and HTTPS protocols are supported", null, false);
            }
            
            // Convert to URL to use the openConnection method
            URL validUrl = uri.toURL();
            
            // Check if the URL is reachable
            HttpURLConnection connection = (HttpURLConnection) validUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            
            // Get the response code
            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "unknown/unknown";
            }
            
            // Check if the URL is valid based on the response code
            if (responseCode >= 200 && responseCode < 300) {
                return new ValidationResult(url, "Valid", "HTTP " + responseCode + " " + connection.getResponseMessage(), 
                    contentType, true);
            } else if (responseCode >= 300 && responseCode < 400) {
                return new ValidationResult(url, "Warning", "HTTP " + responseCode + " (Redirect) " + connection.getResponseMessage(), 
                    contentType, true);
            } else {
                return new ValidationResult(url, "Error", "HTTP " + responseCode + " " + connection.getResponseMessage(), 
                    contentType, false);
            }
        } catch (MalformedURLException e) {
            logger.error("Malformed URL: {}", url, e);
            return new ValidationResult(url, "Error", "Malformed URL: " + e.getMessage(), "unknown/unknown", false);
        } catch (URISyntaxException e) {
            logger.error("Invalid URI syntax: {}", url, e);
            return new ValidationResult(url, "Error", "Invalid URI syntax: " + e.getMessage(), "unknown/unknown", false);
        } catch (SocketTimeoutException e) {
            logger.error("Timeout validating URL: {}", url, e);
            return new ValidationResult(url, "Error", "Connection timeout: " + e.getMessage(), "unknown/unknown", false);
        } catch (IOException e) {
            logger.error("I/O error validating URL: {}", url, e);
            return new ValidationResult(url, "Error", "Connection error: " + e.getMessage(), "unknown/unknown", false);
        } catch (Exception e) {
            logger.error("Unexpected error validating URL: {}", url, e);
            return new ValidationResult(url, "Error", "Validation error: " + e.getMessage(), "unknown/unknown", false);
        }
    }

    @Override
    public EnhancedValidationResult validateUrlWithAI(String url) {
        // First perform basic validation
        ValidationResult basicResult = validateUrl(url);
        
        // Create enhanced result
        EnhancedValidationResult result = new EnhancedValidationResult(
            basicResult.getUrl(),
            basicResult.getStatus(),
            basicResult.getMessage()
        );
        
        try {
            // Generate a unique ID if not already set
            result.setUrlId(UUID.randomUUID().toString());
            
            // Extract name from URL
            result.setUrlName(extractUrlName(url));
            
            // If the basic validation failed, return the basic result
            if (!"Valid".equals(basicResult.getStatus()) && !"Warning".equals(basicResult.getStatus())) {
                return result;
            }
            
            // Get more details about the URL
            URL validUrl = new URI(url).toURL();
            HttpURLConnection connection = (HttpURLConnection) validUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // Set response code
            result.setStatusCode(connection.getResponseCode());
            
            // Get content type
            String contentType = connection.getContentType();
            result.setContentType(contentType != null ? contentType : "unknown");
            
            // Detect asset type - Check if it's a SpaceNews URL first
            if (url.toLowerCase().contains("spacenews.com")) {
                // Use specialized classification for SpaceNews URLs
                String spaceNewsType = detectSpaceNewsContentType(url);
                result.setAssetType(spaceNewsType);
            } else {
                // Use general asset type detection for non-SpaceNews URLs
                String assetType = detectAssetType(url, contentType);
                result.setAssetType(assetType);
            }
            
            // For HTML pages, try to extract the title
            if ("webpage".equals(result.getAssetType()) || 
                result.getAssetType().contains("-page") || 
                result.getAssetType().contains("-section") || 
                "article".equals(result.getAssetType()) || 
                "commentary".equals(result.getAssetType()) || 
                "feature".equals(result.getAssetType()) || 
                "homepage".equals(result.getAssetType())) {
                
                // Make a new connection to get content
                connection = (HttpURLConnection) validUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    
                    // Read the first 1000 lines or until we find the title
                    String line;
                    int lineCount = 0;
                    String title = null;
                    
                    while ((line = reader.readLine()) != null && lineCount < 1000) {
                        lineCount++;
                        
                        // Look for HTML title tag
                        Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>", 
                                               Pattern.CASE_INSENSITIVE).matcher(line);
                        if (titleMatcher.find()) {
                            title = titleMatcher.group(1).trim();
                            break;
                        }
                        
                        // Also look for meta title tags
                        Matcher metaTitleMatcher = Pattern.compile(
                                "<meta(?:\\s+[^>]*?)?\\s+property\\s*=\\s*[\"']og:title[\"']\\s+content\\s*=\\s*[\"'](.*?)[\"']",
                                Pattern.CASE_INSENSITIVE).matcher(line);
                        if (metaTitleMatcher.find()) {
                            title = metaTitleMatcher.group(1).trim();
                            break;
                        }
                    }
                    
                    // Set the title if found
                    if (title != null && !title.isEmpty()) {
                        result.setUrlName(title);
                    }
                } catch (Exception e) {
                    logger.debug("Error extracting title from HTML: {}", e.getMessage());
                    // If we can't extract title, just continue with the URL name
                }
            } else if ("image".equals(result.getAssetType())) {
                // For images, fetch dimensions if possible
                try {
                    // Try to get content length
                    int contentLength = connection.getContentLength();
                    if (contentLength > 0) {
                        result.addMetadata("size", String.valueOf(contentLength));
                    }
                    
                    // Try to determine image format from content type
                    if (contentType != null) {
                        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                            result.addMetadata("format", "JPEG");
                        } else if (contentType.contains("png")) {
                            result.addMetadata("format", "PNG");
                        } else if (contentType.contains("gif")) {
                            result.addMetadata("format", "GIF");
                        } else if (contentType.contains("svg")) {
                            result.addMetadata("format", "SVG");
                        }
                    }
                    
                    // Improve title for image assets
                    String extractedName = result.getUrlName();
                    String format = result.getMetadata().get("format");
                    if (format != null) {
                        extractedName = extractedName + " [" + format + "]";
                    }
                    result.setUrlName(extractedName);
                } catch (Exception e) {
                    logger.debug("Error getting image metadata: {}", e.getMessage());
                }
            } else if ("document".equals(result.getAssetType())) {
                // For documents, add document type information
                String docType = "";
                if (url.toLowerCase().endsWith(".pdf")) {
                    docType = "PDF";
                } else if (url.toLowerCase().endsWith(".doc") || url.toLowerCase().endsWith(".docx")) {
                    docType = "Word";
                } else if (url.toLowerCase().endsWith(".ppt") || url.toLowerCase().endsWith(".pptx")) {
                    docType = "PowerPoint";
                } else if (url.toLowerCase().endsWith(".xls") || url.toLowerCase().endsWith(".xlsx")) {
                    docType = "Excel";
                }
                
                if (!docType.isEmpty()) {
                    result.addMetadata("docType", docType);
                    String extractedName = result.getUrlName();
                    result.setUrlName(extractedName + " [" + docType + "]");
                }
            }
            
            // Add timestamp
            result.setValidatedAt(LocalDateTime.now());
            
            // Add SpaceNews-specific metadata for those URLs
            if (url.toLowerCase().contains("spacenews.com")) {
                result.addMetadata("source", "SpaceNews");
                result.addMetadata("contentType", result.getAssetType());
                
                // Add additional context based on the content type
                if ("article".equals(result.getAssetType())) {
                    result.addMetadata("content_category", "News Article");
                } else if ("commentary".equals(result.getAssetType())) {
                    result.addMetadata("content_category", "Opinion/Commentary");
                } else if ("feature".equals(result.getAssetType())) {
                    result.addMetadata("content_category", "Feature");
                } else if ("press-release".equals(result.getAssetType())) {
                    result.addMetadata("content_category", "Press Release");
                }
            }
            
            // For AI enhancement, we would integrate with an AI service here
            if (aiMemoryService != null) {
                try {
                    // Record validation in AI memory
                    Map<String, Object> aiContext = new HashMap<>();
                    aiContext.put("url", url);
                    aiContext.put("contentType", contentType);
                    aiContext.put("assetType", result.getAssetType());
                    
                    // In a production implementation, aiMemoryService would have methods
                    // to analyze and classify the content
                    // This is placeholder for future AI integration
                    result.addMetadata("ai_integrated", "true");
                    
                } catch (Exception e) {
                    logger.warn("Error in AI enhancement: {}", e.getMessage());
                    result.addMetadata("ai_integrated", "error");
                    // Continue with basic validation result
                }
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error in AI validation for URL: {}", url, e);
            result.addMetadata("validation_error", e.getMessage());
            return result;
        }
    }
    
    @Override
    public List<ValidationResult> validateUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return new ArrayList<>();
        }
        
        logger.info("Validating batch of {} URLs", urls.size());
        
        // Create a list to hold the futures
        List<CompletableFuture<ValidationResult>> futures = urls.stream()
            .map(url -> CompletableFuture.supplyAsync(() -> validateUrl(url), executorService))
            .collect(Collectors.toList());
        
        // Wait for all futures to complete and collect results
        return futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
    }
    
    @Override
    public ValidatedUrlCollection createValidationFromSiteMap(String siteMapId, String name) {
        logger.info("Creating validation collection from site map: {}", siteMapId);
        
        // Create validation collection
        ValidatedUrlCollection collection = new ValidatedUrlCollection(name, siteMapId);
        
        // Save to storage
        storageService.save(collection, collection.getId(), STEP_VALIDATION);
        
        return collection;
    }
    
    @Override
    public ValidatedUrlCollection createValidationWithUrls(String siteMapId, String name, List<String> urls) {
        logger.info("Creating validation collection with {} explicit URLs for site map: {}", urls.size(), siteMapId);
        
        // Create validation collection
        ValidatedUrlCollection collection = new ValidatedUrlCollection(name, siteMapId);
        
        // Store the URLs explicitly in the collection
        collection.setExplicitUrls(urls);
        
        // Save to storage
        storageService.save(collection, collection.getId(), STEP_VALIDATION);
        
        return collection;
    }
    
    @Override
    public ValidatedUrlCollection getValidationCollection(String validationId) {
        ValidatedUrlCollection collection = loadValidationCollection(validationId);
        if (collection == null) {
            logger.warn("Failed to load validation collection: {}. Attempting with retry logic.", validationId);
            return loadValidationCollectionWithRetry(validationId);
        }
        return collection;
    }
    
    /**
     * Loads a validation collection with retry logic and field mapping correction.
     * This method attempts to load the collection using standard methods first,
     * and if that fails, tries more resilient approaches.
     *
     * @param validationId The validation ID
     * @return The loaded validation collection, or null if loading fails after all retries
     */
    private ValidatedUrlCollection loadValidationCollectionWithRetry(String validationId) {
        // Try standard loading from storage service first
        ValidatedUrlCollection collection = storageService.load(validationId, STEP_VALIDATION, ValidatedUrlCollection.class);
        
        if (collection != null) {
            // Check if we have URLs in the collection
            if (collection.getUrls() == null || collection.getUrls().isEmpty()) {
                logger.info("Collection loaded but URLs array is empty, attempting to load URLs separately");
                
                // Try to load URLs array separately
                try {
                    File urlsFile = new File(Paths.get("data", validationId, "urls.json").toString());
                    if (urlsFile.exists()) {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        
                        // Try to load as array
                        List<EnhancedValidationResult> urls = mapper.readValue(urlsFile, 
                            mapper.getTypeFactory().constructCollectionType(List.class, EnhancedValidationResult.class));
                        
                        if (urls != null && !urls.isEmpty()) {
                            collection.setUrls(urls);
                            logger.info("Loaded {} URLs from separate file for collection {}", urls.size(), validationId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error loading URLs separately: {}", e.getMessage());
                }
            }
            
            return collection;
        }
        
        // If standard loading failed, try with manual field mapping
        try {
            // Attempt to load as a generic map to handle field name inconsistencies
            @SuppressWarnings("unchecked")
            Map<String, Object> rawData = storageService.load(validationId, STEP_VALIDATION, Map.class);
            
            if (rawData == null) {
                logger.error("Could not load raw data for validation collection: {}", validationId);
                return null;
            }
            
            // Create a new collection and map fields manually
            ValidatedUrlCollection newCollection = new ValidatedUrlCollection();
            
            // Map standard fields
            if (rawData.containsKey("id")) newCollection.setId((String) rawData.get("id"));
            if (rawData.containsKey("name")) newCollection.setName((String) rawData.get("name"));
            
            // Handle source map ID field - could be either sourceMapId or siteMapId
            if (rawData.containsKey("sourceMapId")) {
                newCollection.setSourceMapId((String) rawData.get("sourceMapId"));
            } else if (rawData.containsKey("siteMapId")) {
                newCollection.setSourceMapId((String) rawData.get("siteMapId"));
            }
            
            // Map other fields as needed
            if (rawData.containsKey("status")) newCollection.setStatus((String) rawData.get("status"));
            if (rawData.containsKey("message")) newCollection.setMessage((String) rawData.get("message"));
            if (rawData.containsKey("totalUrls")) {
                Object totalUrls = rawData.get("totalUrls");
                if (totalUrls instanceof Integer) {
                    newCollection.setTotalUrls((Integer) totalUrls);
                } else if (totalUrls instanceof Number) {
                    newCollection.setTotalUrls(((Number) totalUrls).intValue());
                }
            }
            
            // Try to handle the URLs array
            if (rawData.containsKey("urls") && rawData.get("urls") instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> urlsRaw = (List<Map<String, Object>>) rawData.get("urls");
                    List<EnhancedValidationResult> urls = new ArrayList<>();
                    
                    // Convert each URL map to an EnhancedValidationResult
                    for (Map<String, Object> urlData : urlsRaw) {
                        EnhancedValidationResult result = new EnhancedValidationResult();
                        
                        if (urlData.containsKey("url")) result.setUrl((String) urlData.get("url"));
                        if (urlData.containsKey("status")) result.setStatus((String) urlData.get("status"));
                        if (urlData.containsKey("message")) result.setMessage((String) urlData.get("message"));
                        if (urlData.containsKey("urlName")) result.setUrlName((String) urlData.get("urlName"));
                        if (urlData.containsKey("assetType")) result.setAssetType((String) urlData.get("assetType"));
                        
                        urls.add(result);
                    }
                    
                    newCollection.setUrls(urls);
                    logger.info("Mapped {} URLs from raw data", urls.size());
                } catch (Exception e) {
                    logger.warn("Error mapping URLs from raw data: {}", e.getMessage());
                }
            } else if (rawData.containsKey("urls") && rawData.get("urls") == null) {
                logger.warn("URLs field is null in raw data for validation {}", validationId);
            } else if (!rawData.containsKey("urls")) {
                logger.warn("No URLs field found in raw data for validation {}", validationId);
                
                // Try loading URLs separately if they're in a separate file
                try {
                    File urlsFile = new File(Paths.get("data", validationId, "urls.json").toString());
                    if (urlsFile.exists()) {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        
                        // Try to load as array
                        List<EnhancedValidationResult> urls = mapper.readValue(urlsFile, 
                            mapper.getTypeFactory().constructCollectionType(List.class, EnhancedValidationResult.class));
                        
                        if (urls != null && !urls.isEmpty()) {
                            newCollection.setUrls(urls);
                            logger.info("Loaded {} URLs from separate file for collection {}", urls.size(), validationId);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error loading URLs separately: {}", e.getMessage());
                }
            }
            
            // Save the corrected collection
            storageService.save(newCollection, validationId, STEP_VALIDATION);
            logger.info("Successfully migrated validation collection {} with field corrections", validationId);
            
            return newCollection;
        } catch (Exception e) {
            logger.error("Failed to load validation collection with retry logic: {}", validationId, e);
            return null;
        }
    }
    
    /**
     * Standard method to load validation collection
     */
    private ValidatedUrlCollection loadValidationCollection(String validationId) {
        return storageService.load(validationId, STEP_VALIDATION, ValidatedUrlCollection.class);
    }
    
    @Override
    public List<ValidatedUrlCollection> listValidationCollections() {
        List<String> validationIds = storageService.listIds(STEP_VALIDATION);
        List<ValidatedUrlCollection> collections = new ArrayList<>();
        
        for (String id : validationIds) {
            ValidatedUrlCollection collection = getValidationCollection(id);
            if (collection != null) {
                collections.add(collection);
            }
        }
        
        return collections;
    }
    
    @Override
    public CompletableFuture<ValidatedUrlCollection> startValidation(String validationId, int concurrentRequests, boolean useAI) {
        // Load the validation collection with retries
        ValidatedUrlCollection collection = null;
        int maxRetries = 3;
        int retryCount = 0;
        
        while (collection == null && retryCount < maxRetries) {
            collection = getValidationCollection(validationId);
            if (collection == null) {
                retryCount++;
                if (retryCount < maxRetries) {
                    logger.warn("Validation collection {} not found, retrying ({}/{})", validationId, retryCount, maxRetries);
                    try {
                        // Wait before retrying
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        if (collection == null) {
            logger.error("Cannot start validation: collection {} not found after {} retries", validationId, maxRetries);
            CompletableFuture<ValidatedUrlCollection> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Validation collection not found after " + maxRetries + " retries"));
            return future;
        }
        
        // Store the collection in a final variable for lambda access
        final ValidatedUrlCollection validatedCollection = collection;
        
        // Check if there's already an active validation for this collection
        CompletableFuture<ValidatedUrlCollection> existingValidation = activeValidations.get(validationId);
        if (existingValidation != null && !existingValidation.isDone()) {
            logger.warn("Validation already in progress for collection {}", validationId);
            return existingValidation;
        }
        
        // Create a stop flag for this validation
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        stopFlags.put(validationId, stopFlag);
        
        // Initialize status tracking
        Map<String, Object> status = new HashMap<>();
        status.put("state", "INITIALIZING");
        status.put("progress", 0);
        status.put("total", 0);
        status.put("startTime", System.currentTimeMillis());
        validationStatus.put(validationId, status);
        
        // Create a future that will hold the result
        CompletableFuture<ValidatedUrlCollection> future = new CompletableFuture<>();
        activeValidations.put(validationId, future);
        
        // Start the validation process asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Get URLs to validate - either from explicit list or site map
                List<String> urls;
                if (validatedCollection.getExplicitUrls() != null && !validatedCollection.getExplicitUrls().isEmpty()) {
                    urls = validatedCollection.getExplicitUrls();
                    logger.info("Using {} explicit URLs for validation collection {}", urls.size(), validationId);
                } else {
                    // Load URLs from site map
                    urls = loadUrlsFromSiteMap(validatedCollection.getSiteMapId());
                    logger.info("Loaded {} URLs from site map {} for validation collection {}", 
                        urls.size(), validatedCollection.getSiteMapId(), validationId);
                }
                
                if (urls.isEmpty()) {
                    logger.warn("No URLs found for validation collection {}", validationId);
                    validatedCollection.setStatus("Completed");
                    validatedCollection.setMessage("No URLs found to validate");
                    validatedCollection.setCompletedAt(LocalDateTime.now());
                    storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                    future.complete(validatedCollection);
                    return;
                }
                
                // Update status
                int totalUrls = urls.size();
                Map<String, Object> updatedStatus = validationStatus.get(validationId);
                updatedStatus.put("state", "RUNNING");
                updatedStatus.put("total", totalUrls);
                updatedStatus.put("validated", 0);
                updatedStatus.put("progress", 0);
                
                // Update collection
                validatedCollection.setStatus("In Progress");
                validatedCollection.setMessage("Validating " + totalUrls + " URLs");
                validatedCollection.setTotalUrls(totalUrls);
                validatedCollection.setValidUrls(0);
                validatedCollection.setInvalidUrls(0);
                validatedCollection.setWarningUrls(0);
                storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                
                logger.info("Starting validation of {} URLs for collection {}", totalUrls, validationId);
                
                // Determine if we need to use batching for large datasets
                boolean useBatches = totalUrls > 10000;
                int batchSize = Math.min(concurrentRequests, 10); // Cap at 10 concurrent requests
                int maxBatchSize = 1000; // Process 1000 URLs at a time for very large datasets
                
                // For extremely large datasets, increase parallelism
                if (totalUrls > 1000000) {
                    logger.info("Extremely large dataset detected ({}+ URLs) - using enhanced parallelism", 
                              totalUrls);
                    batchSize = Math.min(20, concurrentRequests); // More concurrent processing
                    maxBatchSize = 2000; // Larger batch size
                }
                
                // Add start time to status for performance tracking
                updatedStatus.put("processingStartTime", System.currentTimeMillis());
                
                AtomicInteger processed = new AtomicInteger(0);
                
                if (useBatches) {
                    // Process in batches for large datasets
                    logger.info("Using batched processing for large dataset of {} URLs", totalUrls);
                    
                    int batches = (int) Math.ceil((double) totalUrls / maxBatchSize);
                    for (int batchIndex = 0; batchIndex < batches; batchIndex++) {
                        if (stopFlag.get()) {
                            logger.info("Validation for collection {} was stopped during batch {}", validationId, batchIndex);
                            break;
                        }
                        
                        int startIndex = batchIndex * maxBatchSize;
                        int endIndex = Math.min(startIndex + maxBatchSize, totalUrls);
                        List<String> batchUrls = urls.subList(startIndex, endIndex);
                        
                        // Track batch start time for performance metrics
                        long batchStartTime = System.currentTimeMillis();
                        
                        logger.info("Processing batch {}/{} with {} URLs (indexes {}-{})", 
                                  batchIndex + 1, batches, batchUrls.size(), startIndex, endIndex - 1);
                        
                        // Update status to show current batch
                        updatedStatus.put("currentBatch", batchIndex + 1);
                        updatedStatus.put("totalBatches", batches);
                        updatedStatus.put("batchSize", batchUrls.size());
                        updatedStatus.put("batchStartTime", batchStartTime);
                        
                        // Process this batch
                        processBatch(batchUrls, validationId, validatedCollection, updatedStatus, processed, 
                                   totalUrls, useAI, batchSize, stopFlag);
                        
                        // Check stop flag after batch processing
                        if (stopFlag.get()) {
                            logger.info("Validation for collection {} was stopped after completing batch {}/{}", 
                                      validationId, batchIndex + 1, batches);
                            break;
                        }
                        
                        // Update status between batches with performance metrics
                        int count = processed.get();
                        double progress = (double) count / totalUrls * 100;
                        long batchEndTime = System.currentTimeMillis();
                        long batchDuration = batchEndTime - batchStartTime;
                        double urlsPerSecond = batchUrls.size() / (Math.max(1, batchDuration / 1000.0));
                        
                        logger.info("Completed batch {}/{}, total progress: {}% ({}/{}), performance: {:.2f} URLs/sec", 
                                  batchIndex + 1, batches, (int)progress, count, totalUrls, urlsPerSecond);
                        
                        // Add performance metrics to status
                        updatedStatus.put("lastBatchDuration", batchDuration);
                        updatedStatus.put("urlsPerSecond", urlsPerSecond);
                        
                        // Save collection after each batch
                        storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                    }
                } else {
                    // Track start time
                    long startTime = System.currentTimeMillis();
                    
                    // Process all URLs at once for smaller datasets
                    logger.info("Processing all {} URLs in a single batch", totalUrls);
                    processBatch(urls, validationId, validatedCollection, updatedStatus, processed, 
                               totalUrls, useAI, batchSize, stopFlag);
                    
                    // Add performance metrics
                    long duration = System.currentTimeMillis() - startTime;
                    double urlsPerSecond = totalUrls / (Math.max(1, duration / 1000.0));
                    updatedStatus.put("totalDuration", duration);
                    updatedStatus.put("urlsPerSecond", urlsPerSecond);
                    
                    logger.info("Completed processing {} URLs in {} ms ({:.2f} URLs/sec)", 
                              processed.get(), duration, urlsPerSecond);
                }
                
                // Check if we were stopped
                if (stopFlag.get()) {
                    // Update collection status to show it was stopped
                    validatedCollection.setStatus("Stopped");
                    validatedCollection.setMessage("Validation stopped by user after processing " + processed.get() + " URLs");
                    validatedCollection.setCompletedAt(LocalDateTime.now());
                    storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                    
                    // Update status
                    updatedStatus.put("state", "STOPPED");
                    updatedStatus.put("endTime", System.currentTimeMillis());
                    
                    logger.info("Validation was stopped after processing {} URLs for collection {}", 
                              processed.get(), validationId);
                } else {
                    // Normal completion - update collection status
                    validatedCollection.setStatus("Completed");
                    validatedCollection.setMessage("Validated " + processed.get() + " URLs");
                    validatedCollection.setCompletedAt(LocalDateTime.now());
                    storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                    
                    // Update status
                    updatedStatus.put("state", "COMPLETED");
                    updatedStatus.put("endTime", System.currentTimeMillis());
                    
                    logger.info("Completed validation of {} URLs for collection {}", processed.get(), validationId);
                }
                
                // Complete the future regardless of stop or completion
                future.complete(validatedCollection);
            } catch (Exception e) {
                logger.error("Error in validation process for collection {}", validationId, e);
                
                // Update collection status
                validatedCollection.setStatus("Error");
                validatedCollection.setMessage("Error: " + e.getMessage());
                storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                
                // Update status
                Map<String, Object> updatedStatus = validationStatus.get(validationId);
                updatedStatus.put("state", "ERROR");
                updatedStatus.put("error", e.getMessage());
                updatedStatus.put("endTime", System.currentTimeMillis());
                
                // Complete the future exceptionally
                future.completeExceptionally(e);
            } finally {
                // Clean up
                stopFlags.remove(validationId);
            }
        }, executorService);
        
        return future;
    }
    
    /**
     * Process a batch of URLs for validation.
     * 
     * @param urls The URLs to validate
     * @param validationId The validation ID
     * @param validatedCollection The validation collection
     * @param updatedStatus The status map to update
     * @param processed Counter for processed URLs
     * @param totalUrls Total number of URLs in the entire dataset
     * @param useAI Whether to use AI validation
     * @param batchSize Number of concurrent requests
     * @param stopFlag Flag to indicate if validation should stop
     */
    private void processBatch(List<String> urls, String validationId, ValidatedUrlCollection validatedCollection,
                            Map<String, Object> updatedStatus, AtomicInteger processed, int totalUrls,
                            boolean useAI, int batchSize, AtomicBoolean stopFlag) {
        // Create executor service for parallel processing
        ExecutorService validationExecutor = Executors.newFixedThreadPool(batchSize);
        
        try {
            // Add an explicit check for the stop flag before starting batch processing
            if (stopFlag.get()) {
                logger.info("Stop flag detected before batch processing started for validation {}", validationId);
                return;
            }
            
            // Process all URLs in this batch
            List<CompletableFuture<ValidationResult>> futures = new ArrayList<>();
            
            // Add a counter to check for stop flag periodically during batch setup
            int urlCounter = 0;
            
            for (String url : urls) {
                // Check stop flag periodically during batch setup
                urlCounter++;
                if (urlCounter % 100 == 0 && stopFlag.get()) {
                    logger.info("Stop flag detected during batch setup for validation {} after {} URLs", 
                              validationId, urlCounter);
                    break;
                }
                
                if (stopFlag.get()) {
                    logger.info("Validation for collection {} was stopped", validationId);
                    break;
                }
                
                CompletableFuture<ValidationResult> resultFuture;
                if (useAI) {
                    resultFuture = CompletableFuture.supplyAsync(() -> {
                        // Check stop flag before starting task
                        if (stopFlag.get()) {
                            logger.debug("Task canceled due to stop flag: {}", url);
                            return new EnhancedValidationResult(url, "Canceled", "Validation stopped by user");
                        }
                        
                        EnhancedValidationResult result = validateUrlWithAI(url);
                        return result;
                    }, validationExecutor);
                } else {
                    resultFuture = CompletableFuture.supplyAsync(() -> {
                        // Check stop flag before starting task
                        if (stopFlag.get()) {
                            logger.debug("Task canceled due to stop flag: {}", url);
                            return new ValidationResult(url, "Canceled", "Validation stopped by user");
                        }
                        
                        return validateUrl(url);
                    }, validationExecutor);
                }
                
                // Add a callback to update progress
                resultFuture.thenAccept(result -> {
                    // Skip updates if stop flag is set (minimizes updates during shutdown)
                    if (stopFlag.get()) {
                        return;
                    }
                    
                    int count = processed.incrementAndGet();
                    double progress = (double) count / totalUrls * 100;
                    
                    // Update status - ensure all fields are updated
                    updatedStatus.put("validated", count);
                    updatedStatus.put("progress", (int) progress);
                    updatedStatus.put("lastUpdated", System.currentTimeMillis());
                    
                    // Log progress periodically
                    if (count % 100 == 0 || count == 1 || count == totalUrls) {
                        logger.info("Validation progress: {}/{} URLs processed ({}%) for collection {}", 
                                 count, totalUrls, (int)progress, validationId);
                    }
                    
                    // Add result to collection
                    if (!"Canceled".equals(result.getStatus())) {
                        // Update counts
                        if ("Valid".equals(result.getStatus())) {
                            validatedCollection.setValidUrls(validatedCollection.getValidUrls() + 1);
                        } else if ("Warning".equals(result.getStatus())) {
                            validatedCollection.setWarningUrls(validatedCollection.getWarningUrls() + 1);
                        } else {
                            validatedCollection.setInvalidUrls(validatedCollection.getInvalidUrls() + 1);
                        }
                        
                        // Add result to collection
                        validatedCollection.addResult(result);
                    }
                    
                    // For very large datasets, save less frequently to improve performance
                    boolean shouldSave = (totalUrls < 1000 && count % 10 == 0) ||   // Small datasets: save every 10
                                         (totalUrls < 10000 && count % 100 == 0) || // Medium datasets: save every 100
                                         (totalUrls < 100000 && count % 1000 == 0) || // Large datasets: save every 1000
                                         (count % 10000 == 0) ||                    // Very large datasets: save every 10000
                                         (count == totalUrls) ||                    // Always save on completion
                                         (count <= 20);                             // Save frequently at the beginning for UI feedback
                                       
                    if (shouldSave) {
                        // For large datasets, we'll use a more efficient saving strategy
                        if (totalUrls > 100000) {
                            // Create a copy with just the metadata for periodic saves
                            ValidatedUrlCollection metadataCopy = new ValidatedUrlCollection(
                                validatedCollection.getName(),
                                validatedCollection.getSourceMapId()
                            );
                            metadataCopy.setId(validatedCollection.getId());
                            metadataCopy.setStatus("In Progress");
                            metadataCopy.setMessage("Validated " + count + " of " + totalUrls + " URLs");
                            metadataCopy.setTotalUrls(totalUrls);
                            metadataCopy.setValidUrls(validatedCollection.getValidUrls());
                            metadataCopy.setWarningUrls(validatedCollection.getWarningUrls());
                            metadataCopy.setInvalidUrls(validatedCollection.getInvalidUrls());
                            metadataCopy.setCreatedAt(validatedCollection.getCreatedAt());
                            
                            // For retrieving the latest results during validation, maintain a cache
                            // of the most recent results (last 1000)
                            int resultCount = validatedCollection.getUrls().size();
                            int startIdx = Math.max(0, resultCount - 1000);
                            if (resultCount > startIdx) {
                                List<EnhancedValidationResult> recentResults = 
                                    validatedCollection.getUrls().subList(startIdx, resultCount);
                                metadataCopy.setUrls(new ArrayList<>(recentResults));
                            }
                            
                            // Save the metadata copy
                            storageService.save(metadataCopy, metadataCopy.getId(), STEP_VALIDATION);
                            
                            // For the first batch of results, also save a separate URLs file for faster loading
                            if (count <= 20) {
                                try {
                                    // Save URLs in a separate file
                                    File urlsDir = new File(Paths.get("data", validatedCollection.getId()).toString());
                                    if (!urlsDir.exists()) {
                                        urlsDir.mkdirs();
                                    }
                                    
                                    File urlsFile = new File(urlsDir, "urls.json");
                                    ObjectMapper mapper = new ObjectMapper();
                                    mapper.registerModule(new JavaTimeModule());
                                    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                                    
                                    // Write current URLs to file
                                    mapper.writerWithDefaultPrettyPrinter().writeValue(urlsFile, validatedCollection.getUrls());
                                    logger.info("Saved first batch of validation results to separate file for faster loading");
                                } catch (Exception e) {
                                    logger.warn("Error saving separate URLs file: {}", e.getMessage());
                                }
                            }
                            
                            logger.debug("Saved validation progress metadata at {}/{} URLs", count, totalUrls);
                        } else {
                            // For smaller datasets, save the full collection
                            storageService.save(validatedCollection, validatedCollection.getId(), STEP_VALIDATION);
                            logger.debug("Saved validation progress at {}/{} URLs", count, totalUrls);
                        }
                    }
                    
                    logger.debug("Validated {}/{} URLs for collection {}", count, totalUrls, validationId);
                });
                
                futures.add(resultFuture);
            }
            
            // Check if we should continue based on stop flag
            if (stopFlag.get()) {
                logger.info("Stop flag detected - completing current tasks but not waiting for all to complete");
                
                // Try to cancel uncompleted futures
                for (CompletableFuture<ValidationResult> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
                
                // Don't wait for all futures, just return
                return;
            }
            
            try {
                // Wait for all futures to complete (with timeout)
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(30, java.util.concurrent.TimeUnit.MINUTES)
                    .join();
            } catch (Exception e) {
                logger.error("Error waiting for validation batch to complete: {}", e.getMessage());
            }
        } finally {
            // Shutdown the executor
            try {
                logger.info("Shutting down validation executor");
                validationExecutor.shutdown();
                
                // Allow up to 5 seconds for tasks to complete
                if (!validationExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("Forcing executor shutdown after timeout");
                    validationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while shutting down executor", e);
                validationExecutor.shutdownNow();
            }
        }
    }
    
    private List<String> loadUrlsFromSiteMap(String siteMapId) {
        // Check if there are explicit URLs in the validation collection
        ValidatedUrlCollection collection = getValidationCollection(siteMapId);
        if (collection != null && collection.getExplicitUrls() != null && !collection.getExplicitUrls().isEmpty()) {
            logger.info("Using {} explicit URLs from validation collection {}", 
                collection.getExplicitUrls().size(), siteMapId);
            return new ArrayList<>(collection.getExplicitUrls());
        }
        
        // Otherwise, load URLs from the site map
        logger.info("Loading URLs from site map: {}", siteMapId);
        return storageService.loadSiteMapUrls(siteMapId);
    }
    
    @Override
    public Map<String, Object> getValidationStatus(String validationId) {
        Map<String, Object> status = validationStatus.get(validationId);
        if (status == null) {
            // If no status is found, return a default status map with INITIALIZING state
            Map<String, Object> defaultStatus = new HashMap<>();
            defaultStatus.put("state", "INITIALIZING");
            defaultStatus.put("progress", 0);
            defaultStatus.put("validated", 0);
            defaultStatus.put("total", 0);
            defaultStatus.put("message", "Initializing validation process");
            defaultStatus.put("startTime", System.currentTimeMillis());
            logger.warn("No status found for validation {}, returning default status", validationId);
            return defaultStatus;
        }
        
        // Return a copy of the status to avoid concurrent modification issues
        return new HashMap<>(status);
    }
    
    @Override
    public boolean stopValidation(String validationId) {
        logger.info("Received request to stop validation: {}", validationId);
        
        // Get the stop flag
        AtomicBoolean stopFlag = stopFlags.get(validationId);
        if (stopFlag != null) {
            // Set the stop flag to true
            stopFlag.set(true);
            logger.info("Stop flag set for validation: {}", validationId);
            
            // Update the validation status
            Map<String, Object> status = validationStatus.get(validationId);
            if (status != null) {
                status.put("state", "STOPPING");
                status.put("stopRequestedAt", System.currentTimeMillis());
                logger.info("Updated validation status to STOPPING");
            }
            
            // Try to cancel the active validation future
            CompletableFuture<ValidatedUrlCollection> future = activeValidations.get(validationId);
            if (future != null && !future.isDone()) {
                logger.info("Attempting to cancel the validation future");
                boolean canceled = future.cancel(true);
                logger.info("Future cancellation result: {}", canceled);
            }
            
            return true;
        } else {
            logger.warn("No stop flag found for validation: {}", validationId);
            return false;
        }
    }
    
    @Override
    public String detectAssetType(String url, String contentType) {
        // Simple content type to asset type mapping
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            
            if (lowerContentType.contains("text/html") || lowerContentType.contains("application/xhtml")) {
                // For HTML pages, first check if it's a SpaceNews URL
                if (url != null && url.toLowerCase().contains("spacenews.com")) {
                    return detectSpaceNewsContentType(url);
                }
                return "webpage";
            } else if (lowerContentType.contains("image/")) {
                return "image";
            } else if (lowerContentType.contains("video/")) {
                return "video";
            } else if (lowerContentType.contains("audio/")) {
                return "audio";
            } else if (lowerContentType.contains("application/pdf")) {
                return "document";
            } else if (lowerContentType.contains("application/msword") || 
                       lowerContentType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml") ||
                       lowerContentType.contains("application/vnd.ms-powerpoint") ||
                       lowerContentType.contains("application/vnd.openxmlformats-officedocument.presentationml") ||
                       lowerContentType.contains("application/vnd.ms-excel") ||
                       lowerContentType.contains("application/vnd.openxmlformats-officedocument.spreadsheetml")) {
                return "document";
            } else if (lowerContentType.contains("application/json")) {
                return "data";
            } else if (lowerContentType.contains("application/xml")) {
                return "data";
            } else if (lowerContentType.contains("text/csv")) {
                return "data";
            }
        }
        
        // If content type isn't available or doesn't match known types, try to infer from URL
        String lowerUrl = url.toLowerCase();
        
        // For SpaceNews URLs, use specific classification regardless of extension
        if (url != null && lowerUrl.contains("spacenews.com")) {
            return detectSpaceNewsContentType(url);
        }
        
        if (lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm") || lowerUrl.endsWith(".php") || 
            lowerUrl.endsWith(".aspx") || lowerUrl.endsWith(".jsp")) {
            return "webpage";
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") || 
                   lowerUrl.endsWith(".png") || lowerUrl.endsWith(".gif") || 
                   lowerUrl.endsWith(".svg") || lowerUrl.endsWith(".webp")) {
            return "image";
        } else if (lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".avi") || 
                   lowerUrl.endsWith(".mov") || lowerUrl.endsWith(".webm") ||
                   lowerUrl.endsWith(".mkv")) {
            return "video";
        } else if (lowerUrl.endsWith(".mp3") || lowerUrl.endsWith(".wav") || 
                   lowerUrl.endsWith(".ogg") || lowerUrl.endsWith(".flac")) {
            return "audio";
        } else if (lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".doc") || 
                   lowerUrl.endsWith(".docx") || lowerUrl.endsWith(".ppt") || 
                   lowerUrl.endsWith(".pptx") || lowerUrl.endsWith(".xls") ||
                   lowerUrl.endsWith(".xlsx")) {
            return "document";
        } else if (lowerUrl.endsWith(".json") || lowerUrl.endsWith(".xml") || 
                   lowerUrl.endsWith(".csv") || lowerUrl.endsWith(".xlsx") ||
                   lowerUrl.endsWith(".sql") || lowerUrl.endsWith(".db")) {
            return "data";
        }
        
        // Default - if it doesn't match any known extension, assume it's a webpage
        if (!lowerUrl.contains(".")) {
            return "webpage";
        }
        
        // Default
        return "unknown";
    }
    
    /**
     * Detects the specific content type for SpaceNews URLs.
     * Classifies URLs into categories like homepage, article, commentary, etc.
     * 
     * @param url The URL to analyze
     * @return The detected SpaceNews content type
     */
    public String detectSpaceNewsContentType(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        
        try {
            // Parse URL
            URI uri = new URI(url);
            String path = uri.getPath();
            
            // If it's just the homepage
            if (path == null || path.isEmpty() || path.equals("/")) {
                return "homepage";
            }
            
            // Remove trailing slash if present
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            
            // Check for category pages
            if (path.contains("/category/")) {
                return "category-page";
            }
            
            // Check for author pages
            if (path.contains("/author/")) {
                return "author-page";
            }
            
            // Check for tag pages
            if (path.contains("/tag/")) {
                return "tag-page";
            }
            
            // Check for date archive pages (YYYY/MM/DD/ format)
            if (path.matches("/\\d{4}/\\d{2}/\\d{2}/?")) {
                return "date-archive";
            }
            
            // Check for month archive pages (YYYY/MM/ format)
            if (path.matches("/\\d{4}/\\d{2}/?")) {
                return "month-archive";
            }
            
            // Check for year archive pages (YYYY/ format)
            if (path.matches("/\\d{4}/?")) {
                return "year-archive";
            }
            
            // Check for specific section pages
            if (path.contains("/civil/") || path.equals("/civil")) {
                return "civil-space-section";
            } else if (path.contains("/commercial/") || path.equals("/commercial")) {
                return "commercial-space-section";
            } else if (path.contains("/military/") || path.equals("/military")) {
                return "military-space-section";
            } else if (path.contains("/launch/") || path.equals("/launch")) {
                return "launch-section";
            } else if (path.contains("/opinion/") || path.equals("/opinion")) {
                return "opinion-section";
            }
            
            // Check for specific content types by URL patterns
            if (path.contains("/commentary/") || path.contains("-commentary/") || 
                path.contains("-op-ed/") || path.contains("/op-ed/")) {
                return "commentary";
            }
            
            // Check for podcast/video content
            if (path.contains("/podcast/") || path.contains("/video/") || 
                path.contains("-podcast/") || path.contains("-video/")) {
                return "multimedia";
            }
            
            // Check for events
            if (path.contains("/event/") || path.contains("/events/")) {
                return "event";
            }
            
            // Check for features/special reports
            if (path.contains("/feature/") || path.contains("-feature/") || 
                path.contains("/special-report/") || path.contains("-special-report/")) {
                return "feature";
            }
            
            // Check for press releases
            if (path.contains("/press-release/") || path.contains("/pressrelease/") || 
                path.contains("-press-release/")) {
                return "press-release";
            }
            
            // If it has a slug format (no extension, not a category/tag/date), it's an article
            // SpaceNews articles typically use URL format: domain.com/slug/
            if (!path.contains(".") && path.split("/").length <= 3) {
                return "article";
            }
            
            // Default for other SpaceNews pages
            return "webpage";
            
        } catch (Exception e) {
            logger.debug("Error parsing SpaceNews URL: {}", url, e);
            return "webpage"; // Default on error
        }
    }
    
    @Override
    public String extractUrlName(String url) {
        if (url == null || url.isEmpty()) {
            return "Unnamed URL";
        }
        
        try {
            // Parse the URL using URI first (non-deprecated approach)
            URI uri = new URI(url);
            URL parsedUrl = uri.toURL();
            
            // Get the path
            String path = parsedUrl.getPath();
            
            // If path is empty or just "/", use the host
            if (path == null || path.isEmpty() || path.equals("/")) {
                return parsedUrl.getHost();
            }
            
            // Remove trailing slash if present
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            
            // Extract the last part of the path
            int lastSlashIndex = path.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
                path = path.substring(lastSlashIndex + 1);
            }
            
            // Remove query parameters
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex);
            }
            
            // Remove fragment
            int fragmentIndex = path.indexOf('#');
            if (fragmentIndex > 0) {
                path = path.substring(0, fragmentIndex);
            }
            
            // Replace hyphens and underscores with spaces
            path = path.replace('-', ' ').replace('_', ' ');
            
            // Remove file extension
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > 0) {
                path = path.substring(0, dotIndex);
            }
            
            // Capitalize first letter of each word
            StringBuilder result = new StringBuilder();
            boolean capitalizeNext = true;
            
            for (char c : path.toCharArray()) {
                if (Character.isSpaceChar(c)) {
                    capitalizeNext = true;
                    result.append(c);
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            
            return result.toString();
        } catch (MalformedURLException | URISyntaxException e) {
            // If URL is invalid, return a default name
            return "Unnamed URL";
        }
    }
    
    /**
     * Shuts down the service and releases resources.
     */
    public void shutdown() {
        logger.info("Shutting down URLValidationService");
        executorService.shutdown();
    }
} 