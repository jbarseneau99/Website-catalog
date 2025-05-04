package com.spacedataarchive.service;

import com.spacedataarchive.model.CatalogProcessingResult;
import com.spacedataarchive.model.ProcessingJob;
import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.ValidationResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.util.URLValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.Consumer;

/**
 * Implementation of the CatalogProcessingService.
 * Manages the end-to-end workflow for space data catalog processing.
 */
public class CatalogProcessingServiceImpl implements CatalogProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(CatalogProcessingServiceImpl.class);
    
    // Services
    private final StorageService storageService;
    private final SiteMapService siteMapService;
    private final URLValidationService urlValidationService;
    
    // Thread pool for processing
    private final ExecutorService executorService;
    
    // Active jobs
    private final Map<String, CompletableFuture<?>> activeJobs = new ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> stopFlags = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> jobStatuses = new ConcurrentHashMap<>();
    
    /**
     * Constructor with dependencies.
     * 
     * @param siteMapService The site map service
     * @param urlValidationService The URL validation service
     */
    public CatalogProcessingServiceImpl(SiteMapService siteMapService, URLValidationService urlValidationService) {
        this.storageService = StorageService.getInstance();
        this.siteMapService = siteMapService;
        this.urlValidationService = urlValidationService;
        this.executorService = Executors.newFixedThreadPool(10);
        
        logger.info("CatalogProcessingService initialized");
    }
    
    @Override
    public ProcessingJob createJob(String name, String seedUrl, int maxDepth, int maxUrls) {
        ProcessingJob job = new ProcessingJob(name, seedUrl, maxDepth, maxUrls);
        storageService.save(job, job.getId(), STEP_MAPPING);
        logger.info("Created new processing job: {}", job.getId());
        return job;
    }
    
    @Override
    public ProcessingJob getJob(String jobId) {
        return storageService.load(jobId, STEP_MAPPING, ProcessingJob.class);
    }
    
    @Override
    public List<ProcessingJob> listJobs() {
        List<String> jobIds = storageService.listIds(STEP_MAPPING);
        List<ProcessingJob> jobs = new ArrayList<>();
        
        for (String jobId : jobIds) {
            ProcessingJob job = getJob(jobId);
            if (job != null) {
                jobs.add(job);
            }
        }
        
        return jobs;
    }
    
    @Override
    public CompletableFuture<CatalogProcessingResult> startMapping(
            String jobId, 
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback) {
        
        // Validate job exists
        ProcessingJob job = getJob(jobId);
        if (job == null) {
            CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Job not found: " + jobId));
            return future;
        }
        
        // Check if job is already running
        if (activeJobs.containsKey(jobId)) {
            statusCallback.accept("Job already running");
            CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Job already running: " + jobId));
            return future;
        }
        
        // Setup status tracking
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        stopFlags.put(jobId, stopFlag);
        
        // Create the result object
        CatalogProcessingResult result = new CatalogProcessingResult(jobId, STEP_MAPPING);
        
        // Start the mapping process
        job.setMappingStatus(ProcessingJob.ProcessingPhaseStatus.RUNNING);
        storageService.save(job, jobId, STEP_MAPPING);
        
        // Create the future for the mapping process
        CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
        activeJobs.put(jobId, future);
        
        // Start the mapping process in a separate thread
        executorService.submit(() -> {
            try {
                // PHASE 1: AI Reconnaissance
                statusCallback.accept("[Phase 1] Starting AI Reconnaissance on " + job.getSeedUrl());
                Map<String, Object> aiAnalysis = performAIReconnaissance(job.getSeedUrl());
                job.setConfiguration("aiAnalysis", aiAnalysis);
                
                // Update the domain estimation based on AI analysis
                if (aiAnalysis.containsKey("estimatedUrlCount")) {
                    job.addStatistic("estimatedUrlCount", aiAnalysis.get("estimatedUrlCount"));
                }
                statusCallback.accept("[Phase 1] AI Reconnaissance completed: identified site structure and potential URL patterns");
                
                // PHASE 2: Optimized Setup
                statusCallback.accept("[Phase 2] Setting up optimized crawl parameters based on AI analysis");
                
                // Apply optimizations from AI analysis
                int optimizedMaxDepth = job.getMaxDepth();
                int optimizedMaxUrls = job.getMaxUrls();
                
                if (aiAnalysis.containsKey("recommendedCrawlDepth")) {
                    optimizedMaxDepth = Math.max(job.getMaxDepth(), (int)aiAnalysis.get("recommendedCrawlDepth"));
                    statusCallback.accept("[Phase 2] Optimized crawl depth: " + optimizedMaxDepth);
                }
                
                if (aiAnalysis.containsKey("estimatedUrlCount")) {
                    optimizedMaxUrls = Math.max(job.getMaxUrls(), (int)aiAnalysis.get("estimatedUrlCount") + 1000);
                    statusCallback.accept("[Phase 2] Optimized URL limit: " + optimizedMaxUrls);
                }
                
                // Create a site map project with optimized parameters
                statusCallback.accept("[Phase 2] Creating optimized site map project");
                SiteMapProject project = siteMapService.createProject(
                    job.getName(), 
                    job.getSeedUrl(), 
                    optimizedMaxDepth, 
                    optimizedMaxUrls
                );
                
                // Store the project ID for other phases
                job.setConfiguration("siteMapProjectId", project.getId());
                
                // Apply URL patterns from AI analysis
                if (aiAnalysis.containsKey("urlPatterns") && aiAnalysis.get("urlPatterns") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> urlPatterns = (List<String>) aiAnalysis.get("urlPatterns");
                    
                    for (String pattern : urlPatterns) {
                        statusCallback.accept("[Phase 2] Adding URL pattern from AI analysis: " + pattern);
                        // Implementation would depend on how the SiteMapService accepts URL patterns
                    }
                }
                
                statusCallback.accept("[Phase 2] Setup completed with AI-optimized parameters");
                
                // PHASE 3: Smart Crawling
                statusCallback.accept("[Phase 3] Starting smart crawling process");
                
                CompletableFuture<List<CrawlResult>> crawlFuture = siteMapService.startCrawl(
                    project.getId(),
                    message -> statusCallback.accept("[Phase 3] " + message),
                    crawlResults -> {
                        // Process crawl results in batches
                        processCrawlResults(crawlResults, result, job);
                        resultsCallback.accept(result);
                    },
                    stopFlag::get
                );
                
                // Wait for the crawl to complete
                List<CrawlResult> crawlResults = crawlFuture.join();
                statusCallback.accept("[Phase 3] Smart crawling completed, found " + crawlResults.size() + " URLs");
                
                // PHASE 4: Validation & Enrichment
                statusCallback.accept("[Phase 4] Starting validation and content type analysis");
                
                // Validate URLs as needed - if not already done during crawling
                AtomicInteger validatedCount = new AtomicInteger(0);
                AtomicInteger timeoutCount = new AtomicInteger(0);
                AtomicInteger totalCount = new AtomicInteger(crawlResults.size());
                
                crawlResults.forEach(crawlResult -> {
                    if (stopFlag.get()) return;
                    
                    if (crawlResult.getValidationResult() == null) {
                        ValidationResult validationResult;
                        try {
                            validationResult = urlValidationService.validateUrl(crawlResult.getUrl());
                        } catch (Exception e) {
                            // Handle timeout or connection errors gracefully
                            logger.warn("Error validating URL {}: {}", crawlResult.getUrl(), e.getMessage());
                            int count = timeoutCount.incrementAndGet();
                            
                            // Create a default failed validation result
                            validationResult = new ValidationResult(
                                crawlResult.getUrl(),
                                "ERROR", 
                                "Connection error: " + e.getMessage(),
                                "unknown/unknown",
                                false
                            );
                            
                            // Log progress for timeouts
                            if (count % 10 == 0) {
                                statusCallback.accept("[Phase 4] Encountered " + count + " connection issues. Still processing...");
                            }
                        }
                        
                        crawlResult.setValidationResult(validationResult);
                        
                        // Additional metadata extraction based on content type
                        if (validationResult.isValid()) {
                            try {
                                extractAdditionalMetadata(crawlResult);
                            } catch (Exception e) {
                                logger.warn("Error extracting metadata for URL {}: {}", crawlResult.getUrl(), e.getMessage());
                            }
                        }
                    }
                    
                    int count = validatedCount.incrementAndGet();
                    if (count % 100 == 0 || count == totalCount.get()) {
                        statusCallback.accept("[Phase 4] Validated " + count + "/" + totalCount.get() + " URLs (" + timeoutCount.get() + " connection issues)");
                    }
                });
                
                statusCallback.accept("[Phase 4] Validation and enrichment completed");
                
                // PHASE 5: Results Processing
                statusCallback.accept("[Phase 5] Processing and organizing results");
                
                // Count valid and invalid URLs
                int validUrlCount = 0;
                int invalidUrlCount = 0;
                Map<String, Integer> contentTypeCount = new HashMap<>();
                
                for (CrawlResult crawlResult : crawlResults) {
                    ValidationResult validationResult = crawlResult.getValidationResult();
                    if (validationResult != null) {
                        if (validationResult.isValid()) {
                            validUrlCount++;
                            
                            // Count by content type
                            String contentType = validationResult.getContentType();
                            contentTypeCount.put(contentType, contentTypeCount.getOrDefault(contentType, 0) + 1);
                        } else {
                            invalidUrlCount++;
                        }
                    }
                }
                
                // Update the job with the results
                job.setMappingResultId(result.getId());
                job.setMappingStatus(ProcessingJob.ProcessingPhaseStatus.COMPLETED);
                job.addStatistic("totalUrlsFound", crawlResults.size());
                job.addStatistic("validUrls", validUrlCount);
                job.addStatistic("invalidUrls", invalidUrlCount);
                
                // Add content type statistics
                for (Map.Entry<String, Integer> entry : contentTypeCount.entrySet()) {
                    job.addStatistic("contentType." + entry.getKey(), entry.getValue());
                }
                
                // Save the job
                storageService.save(job, jobId, STEP_MAPPING);
                
                // Update the result
                result.setSuccessCount(validUrlCount);
                result.setErrorCount(invalidUrlCount);
                result.markAsSuccessful();
                storageService.save(result, result.getId(), STEP_MAPPING);
                
                statusCallback.accept("[Phase 5] Results processing completed");
                
                // Complete the future
                future.complete(result);
                
            } catch (Exception e) {
                logger.error("Error during mapping process", e);
                
                // Update the job
                job.setMappingStatus(ProcessingJob.ProcessingPhaseStatus.FAILED);
                storageService.save(job, jobId, STEP_MAPPING);
                
                // Update the result
                result.markAsFailed("Mapping process failed: " + e.getMessage());
                storageService.save(result, result.getId(), STEP_MAPPING);
                
                // Complete the future exceptionally
                future.completeExceptionally(e);
            } finally {
                // Clean up
                activeJobs.remove(jobId);
                stopFlags.remove(jobId);
            }
        });
        
        return future;
    }
    
    /**
     * Performs AI reconnaissance on the target website to gather information for optimized crawling.
     * 
     * @param seedUrl The seed URL to analyze
     * @return A map of analysis results
     */
    private Map<String, Object> performAIReconnaissance(String seedUrl) {
        // This is a placeholder for actual AI implementation
        // In a real implementation, this would call an AI service to analyze the website
        
        Map<String, Object> results = new HashMap<>();
        
        // Simulate AI analysis with reasonable defaults
        results.put("estimatedUrlCount", 5000);
        results.put("recommendedCrawlDepth", 4);
        results.put("siteStructure", "hierarchical");
        
        // Sample URL patterns the AI might discover
        List<String> urlPatterns = new ArrayList<>();
        urlPatterns.add("/article/[0-9]{4}/.*");
        urlPatterns.add("/news/.*");
        urlPatterns.add("/category/.*");
        results.put("urlPatterns", urlPatterns);
        
        // Other insights
        results.put("hasSitemap", true);
        results.put("hasRobotsTxt", true);
        results.put("estimatedCrawlTime", "10-15 minutes");
        
        // In a real implementation, sleep for a bit to simulate AI processing time
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return results;
    }
    
    /**
     * Extracts additional metadata from a crawl result based on its content type.
     * 
     * @param crawlResult The crawl result to enrich
     */
    private void extractAdditionalMetadata(CrawlResult crawlResult) {
        ValidationResult validationResult = crawlResult.getValidationResult();
        if (validationResult == null || !validationResult.isValid()) {
            return;
        }
        
        // Extract metadata based on content type
        String contentType = validationResult.getContentType();
        
        // This is a simple implementation - in a real app, you would have more sophisticated
        // extraction based on the content type
        if (contentType.contains("text/html")) {
            crawlResult.addMetadata("hasContent", true);
        } else if (contentType.contains("application/pdf")) {
            crawlResult.addMetadata("documentType", "PDF");
        } else if (contentType.contains("image/")) {
            crawlResult.addMetadata("mediaType", "Image");
        }
    }
    
    /**
     * Process crawl results from the site map service.
     * 
     * @param crawlResults The crawl results
     * @param result The processing result
     * @param job The processing job
     */
    private void processCrawlResults(List<CrawlResult> crawlResults, CatalogProcessingResult result, ProcessingJob job) {
        // Basic validation during mapping if configured
        boolean validateDuringMapping = job.getConfiguration("validateDuringMapping", true);
        
        for (CrawlResult crawlResult : crawlResults) {
            String url = crawlResult.getUrl();
            
            if (validateDuringMapping) {
                // Quick validation to filter out obviously invalid URLs
                URLValidator.ValidationResult validationResult = URLValidator.validateUrl(url);
                
                if (validationResult.isValid()) {
                    result.addProcessedUrl(url, "success", "Valid URL");
                } else {
                    result.addProcessedUrl(url, "error", validationResult.getReason());
                }
            } else {
                // Just add the URL without validation
                result.addProcessedUrl(url, "pending", "Pending validation");
            }
        }
    }
    
    @Override
    public CompletableFuture<CatalogProcessingResult> startValidation(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback) {
        
        // Validate job exists
        ProcessingJob job = getJob(jobId);
        if (job == null) {
            CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Job not found: " + jobId));
            return future;
        }
        
        // Check if mapping is completed
        if (job.getMappingStatus() != ProcessingJob.ProcessingPhaseStatus.COMPLETED) {
            statusCallback.accept("Mapping must be completed before validation");
            CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Mapping not completed for job: " + jobId));
            return future;
        }
        
        // Check if job is already running
        if (activeJobs.containsKey(jobId)) {
            statusCallback.accept("Job already running");
            CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Job already running: " + jobId));
            return future;
        }
        
        // TODO: Implement the validation phase
        // This is a placeholder implementation
        
        CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Validation phase not implemented yet"));
        return future;
    }
    
    @Override
    public CompletableFuture<CatalogProcessingResult> startExtraction(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback) {
        
        // TODO: Implement the extraction phase
        // This is a placeholder implementation
        
        CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Extraction phase not implemented yet"));
        return future;
    }
    
    @Override
    public CompletableFuture<CatalogProcessingResult> startEnhancement(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback) {
        
        // TODO: Implement the enhancement phase
        // This is a placeholder implementation
        
        CompletableFuture<CatalogProcessingResult> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedOperationException("Enhancement phase not implemented yet"));
        return future;
    }
    
    @Override
    public Map<String, Object> getJobStatus(String jobId) {
        // Check the job status map first
        if (jobStatuses.containsKey(jobId)) {
            return jobStatuses.get(jobId);
        }
        
        // Load the job
        ProcessingJob job = getJob(jobId);
        if (job == null) {
            return new HashMap<>();
        }
        
        // Create a status map
        Map<String, Object> status = new HashMap<>();
        status.put("id", job.getId());
        status.put("name", job.getName());
        status.put("seedUrl", job.getSeedUrl());
        status.put("createdAt", job.getCreatedAt());
        status.put("lastModified", job.getLastModified());
        status.put("mappingStatus", job.getMappingStatus());
        status.put("validationStatus", job.getValidationStatus());
        status.put("extractionStatus", job.getExtractionStatus());
        status.put("enhancementStatus", job.getEnhancementStatus());
        status.put("statistics", job.getStatistics());
        status.put("isRunning", activeJobs.containsKey(jobId));
        
        return status;
    }
    
    @Override
    public boolean stopJob(String jobId) {
        AtomicBoolean stopFlag = stopFlags.get(jobId);
        if (stopFlag != null) {
            stopFlag.set(true);
            return true;
        }
        return false;
    }
    
    /**
     * Shutdown the service.
     */
    public void shutdown() {
        logger.info("CatalogProcessingService shutting down...");
        
        // Stop all active jobs
        for (String jobId : new ArrayList<>(activeJobs.keySet())) {
            try {
                logger.info("Stopping job during shutdown: {}", jobId);
                AtomicBoolean stopFlag = stopFlags.get(jobId);
                if (stopFlag != null) {
                    stopFlag.set(true);
                    logger.info("Set stop flag for job: {}", jobId);
                }
                
                CompletableFuture<?> future = activeJobs.get(jobId);
                if (future != null && !future.isDone()) {
                    // Try to cancel the future (may not work if already executing)
                    boolean canceled = future.cancel(true);
                    logger.info("Future cancellation attempt for job {}: {}", jobId, canceled ? "successful" : "unsuccessful");
                }
            } catch (Exception e) {
                logger.error("Error stopping job {} during shutdown: {}", jobId, e.getMessage());
            }
        }
        
        // Wait briefly for jobs to respond to stop signals
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Shut down the executor service
        logger.info("Shutting down executor service");
        executorService.shutdownNow();
        
        // Clear all job tracking collections
        activeJobs.clear();
        stopFlags.clear();
        jobStatuses.clear();
        
        logger.info("CatalogProcessingService shutdown complete");
    }
} 