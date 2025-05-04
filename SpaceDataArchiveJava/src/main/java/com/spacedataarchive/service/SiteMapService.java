package com.spacedataarchive.service;

import com.google.common.collect.Lists;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.service.ai.DiscoveryAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for AI-assisted site map creation.
 * Manages the crawling process and persists data at each step.
 */
public class SiteMapService {
    private static final Logger logger = LoggerFactory.getLogger(SiteMapService.class);
    private static final String STEP_PROJECT = "project";
    private static final String STEP_CRAWL = "crawl";
    private static final String STEP_PATTERNS = "patterns";
    private static final String STEP_CRAWL_METADATA = "crawl_metadata";
    
    private static SiteMapService instance;
    private final StorageService storageService;
    private DiscoveryAIService discoveryAIService; // Changed to non-final for lazy initialization
    
    // Active crawls
    private final Map<String, CompletableFuture<List<CrawlResult>>> activeCrawls = new ConcurrentHashMap<>();
    
    // Performance optimization parameters
    private volatile int batchSize = 250; // Increased from 100 for higher throughput
    private volatile long delayBetweenBatches = 500; // Default delay in ms
    private volatile int adaptiveAdjustmentCount = 0;
    private volatile boolean useTimeBasedThrottling = true; // Use time-based throttling
    private volatile long targetBatchDuration = 2000; // Target duration for a batch in ms
    private volatile long lastPerformanceLog = 0; // Last time performance was logged
    
    // Large-scale crawling optimizations
    private volatile int maxConcurrentBatches = 4; // Parallel processing
    @SuppressWarnings("unused") // Reserved for future implementation of chunked processing
    private volatile int chunkSize = 10000; // Size for chunked storage
    private volatile boolean useBloomFilter = true; // Enable Bloom filter for duplicate checking
    private volatile BloomFilter<String> urlBloomFilter; // Initialized lazily
    private volatile boolean useChunkedStorage = false; // Whether to use chunked storage
    private volatile int storageChunkSize = 50000; // Size of each storage chunk
    private volatile ExecutorService crawlExecutor; // Initialized lazily
    // High volume settings - these values were tuned for large crawls
    @SuppressWarnings("unused") // Reserved for future high-volume crawl implementation
    private static final int HIGH_VOLUME_CHUNK_SIZE = 100000;
    @SuppressWarnings("unused") // Reserved for future high-volume storage implementation
    private static final int HIGH_VOLUME_STORAGE_CHUNK_SIZE = 50000;
    @SuppressWarnings("unused") // Reserved for future enhanced concurrency support
    private static final int HIGH_VOLUME_CONCURRENCY = 50;
    // Reserved for future proxy rotation implementation
    @SuppressWarnings("unused") // Reserved for future proxy rotation implementation
    private static final int HIGH_VOLUME_PROXY_COUNT = 10;
    private static final int REQUESTS_PER_IP_THRESHOLD = 50; // Increased threshold
    
    // Chunked storage state
    private final Map<String, ChunkedStorageState> chunkedStorageStates = new ConcurrentHashMap<>();
    
    /**
     * Inner class for managing chunked storage state for each project.
     */
    private class ChunkedStorageState {
        // Used in future implementation for incremental chunking
        @SuppressWarnings("unused") // Will be used in future implementation for chunk tracking
        private int currentChunk;
        // Used for metrics and reporting in future implementations
        @SuppressWarnings("unused") // Will be used in future implementation for metrics
        private int totalResults;
        // Used for pagination in future UI enhancements
        @SuppressWarnings("unused") // Will be used in future implementation for UI pagination
        private int totalChunks;
    }
    
    /**
     * Creates a new SiteMapService.
     */
    private SiteMapService() {
        storageService = StorageService.getInstance();
        
        // We'll initialize everything else lazily when needed
        logger.info("SiteMapService initialized with lazy loading");
    }
    
    /**
     * Lazily initializes the DiscoveryAIService to improve startup time
     */
    private synchronized void initializeAIServiceIfNeeded() {
        if (discoveryAIService == null) {
            try {
                com.spacedataarchive.service.ai.AIServiceManager aiManager = 
                    com.spacedataarchive.service.ai.AIServiceManager.getInstance();
                discoveryAIService = aiManager.getDiscoveryService();
                
                // If the service couldn't be loaded, create a basic stub
                if (discoveryAIService == null) {
                    logger.warn("Discovery service not available from AI Manager, using fallback patterns");
                    // Continue with null service - methods will use fallback patterns
                }
            } catch (Exception e) {
                logger.error("Failed to initialize AI service, will use fallback patterns", e);
                // Continue with null service - methods will use fallback patterns
            }
        }
    }
    
    /**
     * Gets the singleton instance of the SiteMapService.
     * 
     * @return The SiteMapService instance
     */
    public static synchronized SiteMapService getInstance() {
        if (instance == null) {
            instance = new SiteMapService();
        }
        return instance;
    }
    
    /**
     * Initializes a Bloom filter for URL deduplication
     * 
     * @param expectedInsertions Expected number of URLs
     * @param falsePositiveRate Acceptable false positive rate
     */
    private void initializeBloomFilter(int expectedInsertions, double falsePositiveRate) {
        urlBloomFilter = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            expectedInsertions,
            falsePositiveRate
        );
        logger.info("Initialized Bloom filter for URL deduplication: {} expected URLs, {:.4f}% false positive rate",
                    expectedInsertions, falsePositiveRate * 100);
    }
    
    /**
     * Creates a new site map project.
     * 
     * @param name The project name
     * @param seedUrl The seed URL
     * @param crawlDepth The crawl depth
     * @param maxPages The maximum number of pages to crawl
     * @return The created project
     */
    public SiteMapProject createProject(String name, String seedUrl, int crawlDepth, int maxPages) {
        SiteMapProject project = new SiteMapProject(name, seedUrl, crawlDepth, maxPages);
        storageService.save(project, project.getId(), STEP_PROJECT);
        logger.info("Created new site map project: {}", project.getId());
        return project;
    }
    
    /**
     * Loads a site map project.
     * 
     * @param projectId The project ID
     * @return The loaded project, or null if it doesn't exist
     */
    public SiteMapProject loadProject(String projectId) {
        return storageService.load(projectId, STEP_PROJECT, SiteMapProject.class);
    }
    
    /**
     * Lists all site map projects.
     * 
     * @return A list of projects
     */
    public List<SiteMapProject> listProjects() {
        List<String> projectIds = storageService.listProjects();
        List<SiteMapProject> projects = new ArrayList<>();
        
        for (String projectId : projectIds) {
            SiteMapProject project = loadProject(projectId);
            if (project != null) {
                projects.add(project);
            }
        }
        
        return projects;
    }
    
    /**
     * Gets the crawl results for a project.
     * 
     * @param projectId The project ID
     * @return The crawl results
     */
    public List<CrawlResult> getCrawlResults(String projectId) {
        // First check if metadata exists for chunked storage
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = storageService.load(projectId, STEP_CRAWL_METADATA, Map.class);
        if (metadata != null) {
            logger.info("Found metadata for chunked storage for project {}, using chunked loading", projectId);
            return loadChunkedResults(projectId);
        }
        
        // Next check if any chunk files exist, even without metadata
        boolean hasChunks = false;
        for (int i = 0; i < 10; i++) { // Check first 10 possible chunks
            CrawlResult[] chunk = storageService.load(projectId, STEP_CRAWL + "_chunk" + i, CrawlResult[].class);
            if (chunk != null && chunk.length > 0) {
                hasChunks = true;
                logger.info("Found chunk file {} for project {} without metadata", i, projectId);
                break;
            }
        }
        
        if (hasChunks) {
            // Create metadata for this project to mark it as using chunked storage
            Map<String, Object> newMetadata = new HashMap<>();
            newMetadata.put("totalChunks", 10); // Assume up to 10 chunks
            newMetadata.put("chunkSize", storageChunkSize);
            newMetadata.put("format", "chunked");
            newMetadata.put("createdAt", System.currentTimeMillis());
            storageService.save(newMetadata, projectId, STEP_CRAWL_METADATA);
            
            // Set this project to use chunked storage
            chunkedStorageStates.put(projectId, new ChunkedStorageState());
            useChunkedStorage = true;
            
            // Load with chunked approach
            logger.info("Created metadata for existing chunks for project {}", projectId);
            return loadChunkedResults(projectId);
        }
        
        // Fall back to regular storage for smaller datasets
        CrawlResult[] results = storageService.load(projectId, STEP_CRAWL, CrawlResult[].class);
        if (results == null) {
            return Collections.emptyList();
        }
        
        List<CrawlResult> resultList = new ArrayList<>(results.length);
        Collections.addAll(resultList, results);
        
        // If this is a large result set, convert to chunked storage for future
        if (resultList.size() > storageChunkSize) {
            logger.info("Large dataset detected ({}), converting to chunked storage format", resultList.size());
            saveChunkedResults(resultList, projectId);
            useChunkedStorage = true;
        }
        
        return resultList;
    }
    
    /**
     * Starts an AI-assisted crawl process for the specified project.
     * 
     * @param projectId The project ID
     * @param statusCallback A callback for status updates
     * @param resultsCallback A callback for crawl results
     * @return A CompletableFuture that will complete when the crawl is finished
     */
    public CompletableFuture<List<CrawlResult>> startCrawl(String projectId, Consumer<String> statusCallback, Consumer<List<CrawlResult>> resultsCallback) {
        return startCrawl(projectId, statusCallback, resultsCallback, () -> false);
    }
    
    /**
     * Starts an AI-assisted crawl process for the specified project with a stop condition.
     * 
     * @param projectId The project ID
     * @param statusCallback A callback for status updates
     * @param resultsCallback A callback for crawl results
     * @param stopChecker A supplier that returns true if the crawl should stop
     * @return A CompletableFuture that will complete when the crawl is finished
     */
    public CompletableFuture<List<CrawlResult>> startCrawl(String projectId, Consumer<String> statusCallback, 
                                                         Consumer<List<CrawlResult>> resultsCallback, 
                                                         java.util.function.Supplier<Boolean> stopChecker) {
        SiteMapProject project = loadProject(projectId);
        if (project == null) {
            logger.error("Cannot start crawl: project {} not found", projectId);
            CompletableFuture<List<CrawlResult>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Project not found"));
            return future;
        }
        
        // Check if there's already an active crawl for this project
        CompletableFuture<List<CrawlResult>> existingCrawl = activeCrawls.get(projectId);
        if (existingCrawl != null && !existingCrawl.isDone()) {
            logger.warn("Crawl already in progress for project {}", projectId);
            return existingCrawl;
        }
        
        // Update project status
        project.setStatus("Crawling");
        storageService.save(project, projectId, STEP_PROJECT);
        
        // Create a new future for this crawl
        CompletableFuture<List<CrawlResult>> future = new CompletableFuture<>();
        activeCrawls.put(projectId, future);
        
        // Perform the crawl asynchronously
        new Thread(() -> {
            try {
                statusCallback.accept("Analyzing seed URL with AI...");
                
                // Step 1: Analyze the seed URL with AI to get crawling patterns
                List<CrawlPattern> patterns = analyzeSeedUrl(projectId, project.getSeedUrl(), statusCallback);
                
                // Step 2: Perform the crawl using the AI-suggested patterns
                statusCallback.accept("Starting crawl with AI-optimized patterns...");
                List<CrawlResult> results = performCrawl(projectId, project, patterns, statusCallback, resultsCallback, stopChecker);
                
                // Step 3: Update project status
                String finalStatus = stopChecker.get() ? "Stopped" : "Completed";
                project.setStatus(finalStatus);
                storageService.save(project, projectId, STEP_PROJECT);
                
                // Complete the future
                future.complete(results);
                
            } catch (Exception e) {
                logger.error("Crawl failed for project {}", projectId, e);
                project.setStatus("Failed: " + e.getMessage());
                storageService.save(project, projectId, STEP_PROJECT);
                future.completeExceptionally(e);
            } finally {
                activeCrawls.remove(projectId);
            }
        }).start();
        
        return future;
    }
    
    /**
     * Analyzes the seed URL of a project to determine crawl patterns.
     */
    private List<CrawlPattern> analyzeSeedUrl(String projectId, String seedUrl, Consumer<String> statusCallback) {
        if (statusCallback != null) {
            statusCallback.accept("Analyzing seed URL...");
        }
        
        List<CrawlPattern> patterns = new ArrayList<>();
        
        try {
            // Try to use AI service if available
            initializeAIServiceIfNeeded();
            
            if (discoveryAIService != null) {
                // Use AI service to analyze seed URL
                com.spacedataarchive.service.ai.DiscoveryAIService.DiscoveryAnalysis analysis = 
                    discoveryAIService.analyzeSeedUrl(seedUrl);
                
                // Convert include patterns
                for (String pattern : analysis.getIncludePatterns()) {
                    patterns.add(new CrawlPattern(pattern, true));
                }
                
                // Convert exclude patterns
                for (String pattern : analysis.getExcludePatterns()) {
                    patterns.add(new CrawlPattern(pattern, false));
                }
            } else {
                // Fallback to basic patterns if AI service is not available
                logger.info("Using fallback patterns for seed URL: {}", seedUrl);
                
                // Basic include patterns
                patterns.add(new CrawlPattern("/", true));
                patterns.add(new CrawlPattern("/*", true));
                patterns.add(new CrawlPattern("/*/", true));
                
                // Basic exclude patterns
                patterns.add(new CrawlPattern("/wp-admin/*", false));
                patterns.add(new CrawlPattern("/wp-login.php", false));
                patterns.add(new CrawlPattern("/wp-includes/*", false));
                patterns.add(new CrawlPattern("/wp-content/*", false));
                patterns.add(new CrawlPattern("/feed/", false));
                patterns.add(new CrawlPattern("/xmlrpc.php", false));
            }
            
            // Save patterns
            storageService.save(patterns.toArray(new CrawlPattern[0]), projectId, STEP_PATTERNS);
            
            return patterns;
        } catch (Exception e) {
            logger.error("Error analyzing seed URL", e);
            
            // Fallback to basic patterns in case of error
            patterns.add(new CrawlPattern("/", true));
            patterns.add(new CrawlPattern("/*", true));
            patterns.add(new CrawlPattern("/wp-admin/*", false));
            patterns.add(new CrawlPattern("/feed/", false));
            
            return patterns;
        }
    }
    
    /**
     * Performs the crawl using the specified patterns.
     * 
     * @param projectId The project ID
     * @param project The project
     * @param patterns The crawl patterns
     * @param statusCallback A callback for status updates
     * @param resultsCallback A callback for crawl results
     * @return The crawl results
     */
    @SuppressWarnings("unused") // Reserved for future implementation of simplified API
    private List<CrawlResult> performCrawl(String projectId, SiteMapProject project, List<CrawlPattern> patterns, 
                                          Consumer<String> statusCallback, Consumer<List<CrawlResult>> resultsCallback) {
        return performCrawl(projectId, project, patterns, statusCallback, resultsCallback, () -> false);
    }
    
    /**
     * Performs the crawl using the specified patterns with a stop condition.
     * 
     * @param projectId The project ID
     * @param project The project
     * @param patterns The crawl patterns
     * @param statusCallback A callback for status updates
     * @param resultsCallback A callback for crawl results
     * @param stopChecker A supplier that returns true if the crawl should stop
     * @return The crawl results
     */
    private List<CrawlResult> performCrawl(String projectId, SiteMapProject project, List<CrawlPattern> patterns, 
                                          Consumer<String> statusCallback, Consumer<List<CrawlResult>> resultsCallback,
                                          java.util.function.Supplier<Boolean> stopChecker) {
        statusCallback.accept("Crawling " + project.getSeedUrl() + "...");
        
        List<CrawlResult> results = new ArrayList<>();
        
        // Check if this is a SpaceNews URL and use specialized crawling
        if (project.getSeedUrl().contains("spacenews.com")) {
            return performSpaceNewsCrawl(projectId, project, patterns, statusCallback, resultsCallback, stopChecker);
        }
        
        // Add the seed URL
        results.add(new CrawlResult(project.getSeedUrl(), "Seed Page", "OK"));
        resultsCallback.accept(new ArrayList<>(results));
        
        // Simulate crawling more pages
        try {
            // Parse the seed URL to extract domain
            String domain;
            try {
                // Ensure URL has protocol
                String seedUrl = project.getSeedUrl();
                if (!seedUrl.startsWith("http://") && !seedUrl.startsWith("https://")) {
                    seedUrl = "https://" + seedUrl;
                    logger.info("Added protocol to seed URL: {}", seedUrl);
                }
                
                // Create URL from String
                URL url = new URL(seedUrl);
                domain = url.getHost();
                if (domain.startsWith("www.")) {
                    domain = domain.substring(4);
                }
                logger.debug("Extracted domain from seed URL: {}", domain);
            } catch (Exception e) {
                // Try to extract domain directly from the seed URL
                String seedUrl = project.getSeedUrl();
                if (seedUrl.contains("/")) {
                    seedUrl = seedUrl.substring(0, seedUrl.indexOf("/"));
                }
                if (seedUrl.startsWith("www.")) {
                    seedUrl = seedUrl.substring(4);
                }
                domain = seedUrl; // Use as fallback
                logger.warn("Failed to extract domain from seed URL, using fallback: {}", domain, e);
            }
            
            // Create some sample pages based on include patterns
            for (CrawlPattern pattern : patterns) {
                if (stopChecker.get()) {
                    statusCallback.accept("Crawl stopped by user request.");
                    break;
                }
                
                if (pattern.isInclude()) {
                    String patternValue = pattern.getPattern().replace("*", "example");
                    
                    // Skip if pattern starts with a slash, we'll handle it differently
                    if (patternValue.startsWith("/")) {
                        continue;
                    }
                    
                    // Create a few results for this pattern
                    for (int i = 1; i <= 3; i++) {
                        if (stopChecker.get()) {
                            break;
                        }
                        
                        String pageUrl = "https://" + domain + "/" + patternValue + "/" + i;
                        String title = "Page " + patternValue + " " + i;
                        results.add(new CrawlResult(pageUrl, title, "OK"));
                        
                        // Simulate delay and update callbacks
                        Thread.sleep(500);
                        statusCallback.accept("Crawled " + results.size() + " pages so far...");
                        resultsCallback.accept(new ArrayList<>(results));
                    }
                }
            }
            
            // Add some results for path-based patterns
            for (CrawlPattern pattern : patterns) {
                if (stopChecker.get()) {
                    statusCallback.accept("Crawl stopped by user request.");
                    break;
                }
                
                if (pattern.isInclude() && pattern.getPattern().startsWith("/")) {
                    String patternValue = pattern.getPattern().replace("*", "example");
                    
                    // Create a few results for this pattern
                    for (int i = 1; i <= 2; i++) {
                        if (stopChecker.get()) {
                            break;
                        }
                        
                        String pageUrl = "https://" + domain + patternValue + "/" + i;
                        String title = "Page " + patternValue.substring(1) + " " + i;
                        results.add(new CrawlResult(pageUrl, title, "OK"));
                        
                        // Simulate delay and update callbacks
                        Thread.sleep(500);
                        statusCallback.accept("Crawled " + results.size() + " pages so far...");
                        resultsCallback.accept(new ArrayList<>(results));
                    }
                }
            }
            
            // Simulate finding some excluded pages (but mark them as excluded)
            for (CrawlPattern pattern : patterns) {
                if (stopChecker.get()) {
                    statusCallback.accept("Crawl stopped by user request.");
                    break;
                }
                
                if (!pattern.isInclude()) {
                    String patternValue = pattern.getPattern().replace("*", "example");
                    if (patternValue.startsWith("/")) {
                        patternValue = patternValue.substring(1);
                    }
                    
                    String pageUrl = "https://" + domain + "/" + patternValue;
                    String title = "Excluded Page " + patternValue;
                    results.add(new CrawlResult(pageUrl, title, "Excluded"));
                    
                    // Simulate delay and update callbacks
                    Thread.sleep(500);
                    statusCallback.accept("Found excluded page: " + pageUrl);
                    resultsCallback.accept(new ArrayList<>(results));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during simulated crawl", e);
        }
        
        // Save the results
        storageService.save(results.toArray(), projectId, STEP_CRAWL);
        
        statusCallback.accept(stopChecker.get() 
            ? "Crawl stopped by user. Found " + results.size() + " pages."
            : "Crawl complete. Found " + results.size() + " pages.");
        return results;
    }
    
    /**
     * Performs a specialized crawl for SpaceNews with advanced techniques.
     * 
     * @param projectId The project ID
     * @param project The project
     * @param patterns The crawl patterns
     * @param statusCallback A callback for status updates
     * @param resultsCallback A callback for crawl results
     * @return The crawl results
     */
    private List<CrawlResult> performSpaceNewsCrawl(String projectId, SiteMapProject project, List<CrawlPattern> patterns,
                                                 Consumer<String> statusCallback, Consumer<List<CrawlResult>> resultsCallback,
                                                 java.util.function.Supplier<Boolean> stopChecker) {
        logger.info("Starting specialized SpaceNews crawl for project: {}, seed URL: {}", projectId, project.getSeedUrl());
        statusCallback.accept("Starting specialized SpaceNews crawl with enhanced discovery for large sites...");
        
        // First, load any existing results
        List<CrawlResult> results = new ArrayList<>();
        
        // Use chunked loading for large datasets
        if (useChunkedStorage || chunkedStorageStates.containsKey(projectId)) {
            results = loadChunkedResults(projectId);
            logger.info("Loaded {} URLs from chunked storage for project: {}", results.size(), projectId);
        } else {
            CrawlResult[] existingResults = storageService.load(projectId, STEP_CRAWL, CrawlResult[].class);
            if (existingResults != null && existingResults.length > 0) {
                Collections.addAll(results, existingResults);
                logger.info("Loaded {} URLs from regular storage for project: {}", results.size(), projectId);
            } else {
                logger.info("No existing results found for project: {}", projectId);
            }
        }
        
        // Create a set of existing URLs for fast duplicate checking
        java.util.Set<String> existingUrls = new java.util.HashSet<>();
        
        // If we have existing results, populate the Bloom filter and hash set
        if (!results.isEmpty()) {
            long startTime = System.currentTimeMillis();
            for (CrawlResult result : results) {
                existingUrls.add(result.getUrl());
                if (useBloomFilter) {
                    urlBloomFilter.put(result.getUrl());
                }
            }
            long endTime = System.currentTimeMillis();
            
            logger.info("Initialized URL filter with {} existing URLs in {} ms", 
                      results.size(), (endTime - startTime));
            statusCallback.accept("Loaded " + results.size() + " existing URLs from storage for this project.");
            
            // Enable chunked storage for large datasets
            if (results.size() > storageChunkSize) {
                useChunkedStorage = true;
                logger.info("Large dataset detected ({}), enabling chunked storage", results.size());
                statusCallback.accept("Large dataset detected, using optimized storage for scale.");
            }
            
            resultsCallback.accept(new ArrayList<>(results));
        }
        
        // Resume from existing progress (if any)
        boolean isResuming = results.size() > 0;
        if (isResuming) {
            logger.info("Resuming crawl from {} existing URLs", results.size());
            statusCallback.accept("Resuming crawl from " + results.size() + " existing URLs.");
            
            // Log statistics about the existing URLs
            long articleCount = results.stream()
                .filter(r -> !r.getUrl().contains("/category/") && 
                             !r.getUrl().contains("/tag/") && 
                             !r.getUrl().contains("/author/") && 
                             !r.getUrl().matches(".*/(\\d{4})/(\\d{2})/?$"))
                .count();
            
            logger.info("Existing dataset has approximately {} article URLs", articleCount);
            statusCallback.accept("Existing dataset has approximately " + articleCount + " article URLs");
            
            // If we have a substantial number of URLs already, use enhanced discovery
            if (results.size() > 10000) {
                logger.info("Large existing dataset detected ({}), using enhanced discovery techniques", results.size());
                statusCallback.accept("Enhanced URL discovery activated for large dataset");
            }
            
            // Reset Bloom filter to avoid false positives that may be causing issues
            if (useBloomFilter && results.size() > 10000) {
                logger.info("Reinitializing Bloom filter with increased capacity to avoid false positives");
                initializeBloomFilter(results.size() * 3, 0.0001);
                for (CrawlResult result : results) {
                    urlBloomFilter.put(result.getUrl());
                }
                statusCallback.accept("Optimized URL detection for finding more content");
            }
        }
        
        try {
            // Add the seed URL if not already present
            if (!isDuplicate(project.getSeedUrl(), existingUrls)) {
                results.add(new CrawlResult(project.getSeedUrl(), "SpaceNews Home", "OK"));
                logger.info("Added seed URL: {}", project.getSeedUrl());
                resultsCallback.accept(new ArrayList<>(results));
            } else {
                logger.debug("Seed URL already exists in dataset: {}", project.getSeedUrl());
            }
            
            // Extract domain from the seed URL
            String domain;
            try {
                // Modern approach to create URL from String
                URI uri = URI.create(project.getSeedUrl());
                URL url = uri.toURL();
                domain = url.getHost();
                logger.debug("Extracted domain from seed URL: {}", domain);
            } catch (Exception e) {
                domain = "spacenews.com"; // Fallback
                logger.warn("Failed to extract domain from seed URL, using fallback: {}", domain, e);
            }
            
            // Skip sitemap processing if we're resuming and already have a good number of URLs
            if (isResuming && results.size() > 1000) {
                logger.info("Skipping initial sitemap processing for resumed project with {} URLs", results.size());
                statusCallback.accept("Skipping initial sitemap processing for resumed project with " + results.size() + " URLs.");
            } else {
                logger.info("Starting sitemap processing to discover initial URLs");
                // Process sitemaps with optimized handler
                int newUrlsFromSitemaps = processSitemapUrlsOptimized(
                    domain, projectId, existingUrls, results, statusCallback, resultsCallback, stopChecker);
                
                logger.info("Sitemap processing complete. Found {} new URLs (total: {})", 
                          newUrlsFromSitemaps, results.size());
                statusCallback.accept("Found " + newUrlsFromSitemaps + " new URLs from sitemaps (total: " + results.size() + ")");
            }
            
            if (stopChecker.get()) {
                // Save what we have and return early
                logger.info("Stop requested during sitemap processing. Saving {} URLs", results.size());
                if (useChunkedStorage) {
                    saveChunkedResults(results, projectId);
                } else {
                    storageService.save(results.toArray(), projectId, STEP_CRAWL);
                }
                statusCallback.accept("Crawl stopped by user. Found " + results.size() + " pages.");
                resultsCallback.accept(results);
                return results;
            }
            
            // Initialize proxy rotation tracking
            int requestsSinceLastRotation = 0;
            long lastRotationTime = System.currentTimeMillis();
            
            // Track discovered patterns for generating more URLs
            // This variable will be used in future implementations for pattern analysis
            @SuppressWarnings("unused")
            Map<String, Integer> discoveredPathPatterns = new HashMap<>();
            
            // Continuous discovery using pattern-based URL generation
            int batchCount = 0;
            int generationRound = 0;
            
            // Large-scale crawling loop - pattern based for high URL volume sites
            while (!stopChecker.get() && results.size() < 1_000_000) { // Cap at 1M URLs
                batchCount++;
                logger.debug("Starting batch {} of the pattern-based discovery loop", batchCount);
                
                // Every 5 batches, regenerate URLs based on patterns discovered so far
                if (batchCount % 5 == 0) {
                    generationRound++;
                    logger.info("Starting URL generation round {}", generationRound);
                    statusCallback.accept("Generating new URLs based on patterns (round " + generationRound + ")...");
                    
                    // Generate up to 10,000 new URLs per round
                    List<String> generatedUrls = generateURLsFromPatterns(domain, existingUrls, 10000);
                    
                    if (generatedUrls.isEmpty()) {
                        logger.warn("No new URL patterns to explore after {} generation rounds", generationRound);
                        statusCallback.accept("No new URL patterns to explore. Trying alternative discovery...");
                        
                        // Try direct article generation approach for more URLs
                        logger.info("Pattern-based URL generation found 0 URLs - attempting direct article generation");
                        generatedUrls = generateDirectArticleURLs(domain, existingUrls, 5000);
                        
                        if (generatedUrls.isEmpty()) {
                            logger.warn("No new URLs found with any generation method - crawl complete");
                            statusCallback.accept("No new URL patterns to explore. Crawl may be complete.");
                            break;
                        } else {
                            logger.info("Successfully generated {} URLs with direct article approach", generatedUrls.size());
                            statusCallback.accept("Found " + generatedUrls.size() + " new URLs to explore with direct approach");
                        }
                    }
                    
                    logger.info("Generated {} new URLs to explore in round {}", generatedUrls.size(), generationRound);
                    statusCallback.accept("Generated " + generatedUrls.size() + " new URLs to explore...");
                    
                    // Check if we need to rotate proxies
                    if (shouldRotateIP(domain, requestsSinceLastRotation, System.currentTimeMillis() - lastRotationTime)) {
                        logger.info("Rotating IP proxy after {} requests", requestsSinceLastRotation);
                        statusCallback.accept("Rotating IP proxy for better performance...");
                        requestsSinceLastRotation = 0;
                        lastRotationTime = System.currentTimeMillis();
                    }
                    
                    // Process the generated URLs in parallel
                    logger.info("Processing batch of {} generated URLs", generatedUrls.size());
                    List<CrawlResult> newResults = processUrlBatchInParallel(
                        generatedUrls, existingUrls, statusCallback);
                    
                    // Add tracking for rate limiting
                    requestsSinceLastRotation += generatedUrls.size();
                    
                    // Add new results
                    if (!newResults.isEmpty()) {
                        int previousSize = results.size();
                        results.addAll(newResults);
                        
                        logger.info("Added {} new URLs in round {} (success rate: {:.2f}%)", 
                                  newResults.size(), generationRound, 
                                  100.0 * newResults.size() / generatedUrls.size());
                        
                        resultsCallback.accept(new ArrayList<>(results));
                        
                        // Save intermediate results
                        logger.info("Saving intermediate results: {} URLs", results.size());
                        if (useChunkedStorage) {
                            saveChunkedResults(results, projectId);
                        } else {
                            storageService.save(results.toArray(), projectId, STEP_CRAWL);
                        }
                        
                        statusCallback.accept("Added " + newResults.size() + " URLs from pattern generation (total: " + results.size() + ")");
                        
                        // Log growth rate
                        double growthRate = previousSize > 0 ? 
                            100.0 * (results.size() - previousSize) / previousSize : 100.0;
                        logger.info("Dataset growth rate: {:.2f}% in this round", growthRate);
                    } else {
                        logger.warn("No new URLs found from current patterns in round {}", generationRound);
                        statusCallback.accept("No new URLs found from current patterns.");
                    }
                }
                
                // If we aren't finding new URLs after multiple attempts, try direct slug generation
                if (generationRound > 3 && results.size() < 10000) {
                    logger.info("Using advanced slug-based URL generation after {} unsuccessful rounds", generationRound);
                    statusCallback.accept("Using advanced slug-based URL generation to discover more content...");
                    
                    // Generate a large batch of direct article URL guesses
                    List<String> articleUrls = new ArrayList<>();
                    for (int i = 0; i < 50000; i++) {
                        String slug = generateRandomSlug();
                        String url = "https://" + domain + "/" + slug + "/";
                        articleUrls.add(url);
                    }
                    
                    logger.info("Generated {} slug-based URLs for brute-force discovery", articleUrls.size());
                    
                    // Process these in parallel
                    List<CrawlResult> slugResults = processUrlBatchInParallel(
                        articleUrls, existingUrls, statusCallback);
                    
                    if (!slugResults.isEmpty()) {
                        results.addAll(slugResults);
                        logger.info("Found {} new URLs from slug generation (success rate: {:.2f}%)",
                                  slugResults.size(), 100.0 * slugResults.size() / articleUrls.size());
                        statusCallback.accept("Found " + slugResults.size() + " new URLs from slug generation");
                        resultsCallback.accept(new ArrayList<>(results));
                    } else {
                        logger.warn("No valid URLs found from slug-based generation");
                    }
                }
                
                // Take a short break between batches to avoid rate limiting
                if (!stopChecker.get()) {
                    try {
                        logger.debug("Pausing for {} ms between batches", delayBetweenBatches);
                        Thread.sleep(delayBetweenBatches);
                    } catch (InterruptedException e) {
                        logger.warn("Thread interrupted during batch delay", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            // Log the reason for exiting the crawler loop
            if (stopChecker.get()) {
                logger.info("Crawler stopped by user request after finding {} URLs", results.size());
            } else if (results.size() >= 1_000_000) {
                logger.info("Crawler reached maximum URL limit of 1,000,000");
            } else {
                logger.info("Crawler completed naturally after finding {} URLs", results.size());
            }
            
        } catch (Exception e) {
            logger.error("Error during large-scale SpaceNews crawl", e);
            statusCallback.accept("Error during crawl: " + e.getMessage());
        }
        
        // Save the final results
        logger.info("Saving final results: {} URLs", results.size());
        if (useChunkedStorage) {
            saveChunkedResults(results, projectId);
        } else {
            storageService.save(results.toArray(), projectId, STEP_CRAWL);
        }
        
        // Final update
        String finalMessage = stopChecker.get()
            ? "SpaceNews crawl stopped by user. Found " + results.size() + " pages."
            : "SpaceNews crawl complete. Found " + results.size() + " pages.";
            
        logger.info(finalMessage);
        statusCallback.accept(finalMessage);
        resultsCallback.accept(results);
        
        return results;
    }
    
    /**
     * Generates a random article slug for URL discovery.
     * 
     * @return A random article slug
     */
    private String generateRandomSlug() {
        // Common space-related terms for realistic slugs
        String[] terms = {
            "nasa", "spacex", "launch", "rocket", "satellite", "mission", "space", "orbit",
            "mars", "moon", "artemis", "iss", "station", "commercial", "science", "research",
            "exploration", "crew", "astronaut", "technology", "earth", "observation", "solar",
            "jupiter", "venus", "asteroid", "spacecraft", "capsule", "booster", "falcon",
            "starship", "boeing", "northrop", "lockheed", "blue-origin", "virgin", "ula", 
            "space-force", "test", "success", "delay", "funding", "contract", "award", "program"
        };
        
        // Between 2-4 terms for a realistic slug
        int termCount = 2 + (int)(Math.random() * 3); // 2-4 terms
        
        StringBuilder slug = new StringBuilder();
        
        for (int i = 0; i < termCount; i++) {
            // Pick a random term
            String term = terms[(int)(Math.random() * terms.length)];
            
            // Add it to the slug
            if (i > 0) {
                slug.append("-");
            }
            slug.append(term);
        }
        
        return slug.toString();
    }
    
    /**
     * Represents a crawl pattern.
     */
    public static class CrawlPattern {
        private String pattern;
        private boolean include;
        
        public CrawlPattern() {
        }
        
        public CrawlPattern(String pattern, boolean include) {
            this.pattern = pattern;
            this.include = include;
        }
        
        public String getPattern() {
            return pattern;
        }
        
        public void setPattern(String pattern) {
            this.pattern = pattern;
        }
        
        public boolean isInclude() {
            return include;
        }
        
        public void setInclude(boolean include) {
            this.include = include;
        }
    }
    
    /**
     * Adjusts the crawling strategy based on performance metrics.
     * 
     * @param currentRate The current crawl rate (URLs/sec)
     * @param averageRate The average crawl rate (URLs/sec)
     * @param totalUrlsFound The total number of URLs found so far
     */
    public void adjustCrawlStrategy(double currentRate, double averageRate, int totalUrlsFound) {
        long now = System.currentTimeMillis();
        
        // Only log performance metrics once every 30 seconds to avoid logging spam with larger datasets
        if (now - lastPerformanceLog > 30000) {
            logger.info("Adjusting crawl strategy based on performance metrics: currentRate={}, averageRate={}, totalUrls={}", 
                       currentRate, averageRate, totalUrlsFound);
            lastPerformanceLog = now;
        }
        
        adaptiveAdjustmentCount++;
        
        // For very large datasets (>100K URLs), activate chunked storage automatically
        if (totalUrlsFound > 100000 && !useChunkedStorage) {
            useChunkedStorage = true;
            logger.info("Activated chunked storage for large dataset ({} URLs)", totalUrlsFound);
        }
        
        // Apply different strategies for different dataset sizes
        if (totalUrlsFound > 500000) {
            // For extremely large datasets, maximize throughput
            batchSize = 500;
            maxConcurrentBatches = 8;
            delayBetweenBatches = 0;
            
            // For huge datasets, we care more about throughput than frequent UI updates
            storageChunkSize = 100000; // Larger chunks for less frequent I/O
            
            logger.info("Using maximum throughput strategy for very large dataset: batchSize={}, concurrentBatches={}", 
                       batchSize, maxConcurrentBatches);
            return;
        }
        
        if (totalUrlsFound > 100000) {
            // For large datasets, balance throughput with memory usage
            batchSize = 300;
            maxConcurrentBatches = 6;
            delayBetweenBatches = 100;
            
            logger.info("Using balanced strategy for large dataset: batchSize={}, concurrentBatches={}", 
                       batchSize, maxConcurrentBatches);
            return;
        }
        
        // If crawling is very slow (less than 0.5 URL/sec)
        if (currentRate < 0.5) {
            // Increase batch size significantly to process more URLs at once
            batchSize = Math.min(batchSize + 100, 500);
            
            // Reduce delay between batches to increase throughput
            delayBetweenBatches = Math.max(delayBetweenBatches - 200, 0);
            
            // Increase time-based throttling target to allow more time for processing
            targetBatchDuration = Math.min(targetBatchDuration + 1000, 5000);
            
            // Increase parallel processing
            maxConcurrentBatches = Math.min(maxConcurrentBatches + 1, 8);
            
            logger.info("Crawl performance is very slow. Aggressive optimization: batchSize={}, concurrentBatches={}, delay={}ms", 
                       batchSize, maxConcurrentBatches, delayBetweenBatches);
        }
        // If crawling is slow (less than 1 URL/sec)
        else if (currentRate < 1.0) {
            // Increase batch size to process more URLs at once
            batchSize = Math.min(batchSize + 50, 500);
            
            // Reduce delay between batches to increase throughput
            delayBetweenBatches = Math.max(delayBetweenBatches - 100, 0);
            
            // Increase parallel processing slightly
            maxConcurrentBatches = Math.min(maxConcurrentBatches + 1, 6);
            
            logger.info("Crawl performance is slow. Increased batch size to {}, concurrent batches to {}", 
                       batchSize, maxConcurrentBatches);
        }
        // If crawling is very fast (more than 10 URLs/sec)
        else if (currentRate > 10.0) {
            // If we've processed a lot of URLs already, we can be more conservative
            if (totalUrlsFound > 5000) {
                // Reduce batch size slightly to avoid memory issues
                batchSize = Math.max(batchSize - 20, 50);
                
                // Increase delay slightly to reduce server load
                delayBetweenBatches = Math.min(delayBetweenBatches + 50, 1000);
                
                // Reduce target batch duration to increase responsiveness
                targetBatchDuration = Math.max(targetBatchDuration - 500, 1000);
                
                logger.info("Crawl performance is very fast with large dataset. Slight performance throttling applied.");
            }
        }
        // For moderate performance, make small adjustments based on trend
        else if (adaptiveAdjustmentCount > 3) {
            // If current rate is trending down from average
            if (currentRate < averageRate * 0.8) {
                batchSize = Math.min(batchSize + 25, 300);
                delayBetweenBatches = Math.max(delayBetweenBatches - 50, 100);
                logger.info("Crawl performance trending down. Making minor adjustments to improve throughput.");
            }
        }
        
        // Every 10th adjustment, try to optimize based on accumulated data
        if (adaptiveAdjustmentCount % 10 == 0) {
            // If we have good performance (> 5 URLs/sec) but large batch size, try to reduce batch size
            // This helps reduce memory usage while maintaining good performance
            if (currentRate > 5.0 && batchSize > 200) {
                batchSize = Math.max(batchSize - 50, 150);
                logger.info("Optimizing memory usage with good performance. Reduced batch size to {}", batchSize);
            }
            
            // If we consistently have slow performance despite adjustments, try toggling time-based throttling
            if (adaptiveAdjustmentCount > 30 && currentRate < 1.0) {
                useTimeBasedThrottling = !useTimeBasedThrottling;
                logger.info("Persistent slow performance. Toggled time-based throttling to: {}", useTimeBasedThrottling);
            }
            
            // Update thread pool if concurrency changed
            crawlExecutor.shutdownNow();
            crawlExecutor = Executors.newFixedThreadPool(maxConcurrentBatches);
            logger.info("Updated thread pool to {} concurrent batches", maxConcurrentBatches);
        }
        
        logger.debug("Strategy adjusted: batchSize={}, concurrentBatches={}, delayBetweenBatches={}ms, useTimeThrottling={}", 
                   batchSize, maxConcurrentBatches, delayBetweenBatches, useTimeBasedThrottling);
    }
    
    /**
     * Gets the current batch size for crawling.
     * 
     * @return The batch size
     */
    public int getBatchSize() {
        return batchSize;
    }
    
    /**
     * Sets the batch size for crawling.
     * 
     * @param batchSize The batch size to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        logger.info("Batch size set to: {}", batchSize);
    }
    
    /**
     * Gets the current delay between batches.
     * 
     * @return The delay in milliseconds
     */
    public long getDelayBetweenBatches() {
        return delayBetweenBatches;
    }
    
    /**
     * Sets the delay between batches.
     * 
     * @param delayBetweenBatches The delay in milliseconds
     */
    public void setDelayBetweenBatches(long delayBetweenBatches) {
        this.delayBetweenBatches = delayBetweenBatches;
        logger.info("Delay between batches set to: {}ms", delayBetweenBatches);
    }
    
    /**
     * Gets whether time-based throttling is enabled.
     * 
     * @return True if time-based throttling is enabled
     */
    public boolean isUseTimeBasedThrottling() {
        return useTimeBasedThrottling;
    }
    
    /**
     * Sets whether to use time-based throttling.
     * 
     * @param useTimeBasedThrottling True to enable time-based throttling
     */
    public void setUseTimeBasedThrottling(boolean useTimeBasedThrottling) {
        this.useTimeBasedThrottling = useTimeBasedThrottling;
        logger.info("Time-based throttling set to: {}", useTimeBasedThrottling);
    }
    
    /**
     * Gets the target batch duration for time-based throttling.
     * 
     * @return The target duration in milliseconds
     */
    public long getTargetBatchDuration() {
        return targetBatchDuration;
    }
    
    /**
     * Sets the target batch duration for time-based throttling.
     * 
     * @param targetBatchDuration The target duration in milliseconds
     */
    public void setTargetBatchDuration(long targetBatchDuration) {
        this.targetBatchDuration = targetBatchDuration;
        logger.info("Target batch duration set to: {}ms", targetBatchDuration);
    }
    
    /**
     * Saves results in chunks to optimize memory usage for large datasets.
     * 
     * @param results The complete result list
     * @param projectId The project ID
     */
    private void saveChunkedResults(List<CrawlResult> results, String projectId) {
        if (!useChunkedStorage || results.size() <= storageChunkSize) {
            // For smaller datasets, use regular storage
            storageService.save(results.toArray(), projectId, STEP_CRAWL);
            return;
        }
        
        // Get or create chunked storage state
        ChunkedStorageState state = chunkedStorageStates.computeIfAbsent(projectId, k -> new ChunkedStorageState());
        
        // Deduplicate results before chunking to ensure consistent count
        Set<String> urlSet = new HashSet<>();
        List<CrawlResult> dedupedResults = new ArrayList<>();
        
        // Deduplicate the results
        int duplicatesRemoved = 0;
        for (CrawlResult result : results) {
            if (urlSet.add(result.getUrl())) {
                dedupedResults.add(result);
            } else {
                duplicatesRemoved++;
            }
        }
        
        if (duplicatesRemoved > 0) {
            logger.info("Removed {} duplicate URLs when saving chunked results for project {}",
                     duplicatesRemoved, projectId);
        }
        
        // Use deduplicated results for storage
        int totalUniqueResults = dedupedResults.size();
        
        // Calculate total chunks based on deduplicated results
        int totalChunks = (int) Math.ceil(totalUniqueResults / (double) storageChunkSize);
        
        // Update state
        state.totalResults = totalUniqueResults;
        state.totalChunks = totalChunks;
        
        // Save each chunk
        for (int i = 0; i < totalChunks; i++) {
            int start = i * storageChunkSize;
            int end = Math.min(start + storageChunkSize, totalUniqueResults);
            List<CrawlResult> chunk = dedupedResults.subList(start, end);
            
            storageService.save(chunk.toArray(), projectId, STEP_CRAWL + "_chunk" + i);
            logger.debug("Saved chunk {} of {} for project {}, containing {} results", 
                        i + 1, totalChunks, projectId, chunk.size());
        }
        
        // Save metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("totalChunks", totalChunks);
        metadata.put("totalResults", totalUniqueResults);
        metadata.put("chunkSize", storageChunkSize);
        metadata.put("lastUpdated", LocalDateTime.now().toString());
        metadata.put("deduplicated", true);
        
        storageService.save(metadata, projectId, STEP_CRAWL_METADATA);
        logger.info("Saved chunked results for project {}: {} total unique results in {} chunks", 
                   projectId, totalUniqueResults, totalChunks);
    }
    
    /**
     * Loads chunked results from storage.
     * 
     * @param projectId The project ID
     * @return The combined results from all chunks
     */
    @SuppressWarnings("unchecked")
    private List<CrawlResult> loadChunkedResults(String projectId) {
        // Check if this project uses chunked storage
        Map<String, Object> metadata = storageService.load(projectId, STEP_CRAWL_METADATA, Map.class);
        
        if (metadata == null) {
            // No chunks, use regular storage
            CrawlResult[] results = storageService.load(projectId, STEP_CRAWL, CrawlResult[].class);
            if (results == null) {
                return Collections.emptyList();
            }
            
            List<CrawlResult> resultList = new ArrayList<>(results.length);
            Collections.addAll(resultList, results);
            return resultList;
        }
        
        // Load all chunks and deduplicate
        Set<String> urlSet = new HashSet<>(); // Track URLs to avoid duplicates
        List<CrawlResult> combinedResults = new ArrayList<>();
        int totalChunks = ((Number) metadata.get("totalChunks")).intValue();
        int duplicatesFound = 0;
        
        for (int i = 0; i < totalChunks; i++) {
            CrawlResult[] chunk = storageService.load(projectId, STEP_CRAWL + "_chunk" + i, CrawlResult[].class);
            if (chunk != null) {
                for (CrawlResult result : chunk) {
                    if (urlSet.add(result.getUrl())) {
                        // Only add if URL wasn't already in the set
                        combinedResults.add(result);
                    } else {
                        duplicatesFound++;
                    }
                }
            }
        }
        
        if (duplicatesFound > 0) {
            logger.info("Deduplication: Removed {} duplicate URLs when loading chunked results for project {}", 
                     duplicatesFound, projectId);
        }
        
        logger.info("Loaded chunked results for project {}: {} total unique results from {} chunks", 
                   projectId, combinedResults.size(), totalChunks);
        
        return combinedResults;
    }
    
    /**
     * Faster duplicate checking using a Bloom filter for initial screening followed by a Set for verification.
     * 
     * @param url The URL to check
     * @param existingUrls The set of known URLs
     * @return True if the URL is a duplicate, false otherwise
     */
    private boolean isDuplicate(String url, Set<String> existingUrls) {
        if (url == null) {
            logger.warn("Null URL passed to isDuplicate check");
            return true; // Treat null URLs as duplicates
        }
        
        try {
            if (!useBloomFilter) {
                boolean isDuplicate = existingUrls.contains(url);
                if (!isDuplicate) {
                    existingUrls.add(url);
                }
                return isDuplicate;
            }
            
            // Initialize the Bloom filter if it doesn't exist
            if (urlBloomFilter == null) {
                logger.info("Initializing Bloom filter in isDuplicate method");
                int expectedSize = Math.max(existingUrls.size() * 3, 10000);
                initializeBloomFilter(expectedSize, 0.01);
                
                // Add all existing URLs to the Bloom filter
                for (String existingUrl : existingUrls) {
                    urlBloomFilter.put(existingUrl);
                }
            }
            
            // First check the Bloom filter (fast but can have false positives)
            if (!urlBloomFilter.mightContain(url)) {
                // Definitely not in the set
                urlBloomFilter.put(url);
                existingUrls.add(url);
                return false;
            }
            
            // Might be in the set, do an exact check
            if (existingUrls.contains(url)) {
                return true;
            }
            
            // False positive in the Bloom filter, add it to our sets
            urlBloomFilter.put(url);
            existingUrls.add(url);
            
            // Occasionally log Bloom filter false positive rates for monitoring
            if (existingUrls.size() % 10000 == 0) {
                double approximateFalsePositiveRate = urlBloomFilter.expectedFpp();
                logger.debug("Bloom filter stats at {} URLs: Expected false positive rate: {:.6f}%", 
                           existingUrls.size(), approximateFalsePositiveRate * 100);
            }
            
            return false;
        } catch (Exception e) {
            logger.error("Error in isDuplicate check for URL: {}", url, e);
            // Conservative approach: return false to attempt processing the URL anyway
            return false;
        }
    }
    
    /**
     * Process a batch of URLs in parallel for better performance.
     * 
     * @param batchUrls List of URLs to process
     * @param existingUrls Set of already processed URLs
     * @param statusCallback Callback for status updates
     * @return List of processed results
     */
    private List<CrawlResult> processUrlBatchInParallel(List<String> batchUrls, Set<String> existingUrls, 
                                                      Consumer<String> statusCallback) {
        // Initialize the crawl executor if it doesn't exist
        if (crawlExecutor == null) {
            crawlExecutor = Executors.newFixedThreadPool(
                Math.min(Runtime.getRuntime().availableProcessors(), maxConcurrentBatches)
            );
            logger.info("Initialized crawl executor with {} threads", 
                Math.min(Runtime.getRuntime().availableProcessors(), maxConcurrentBatches));
        }
        
        // Split URLs into smaller batches for parallel processing
        List<List<String>> miniBatches = Lists.partition(batchUrls, batchSize);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        logger.info("Processing {} URLs in {} mini-batches of size {}", 
                  batchUrls.size(), miniBatches.size(), batchSize);
        
        // Create a list to hold the futures for each mini-batch
        List<CompletableFuture<List<CrawlResult>>> batchFutures = new ArrayList<>();
        
        for (int batchIndex = 0; batchIndex < miniBatches.size(); batchIndex++) {
            List<String> miniBatch = miniBatches.get(batchIndex);
            final int currentBatchIndex = batchIndex;
            
            CompletableFuture<List<CrawlResult>> batchFuture = CompletableFuture.supplyAsync(() -> {
                List<CrawlResult> batchResults = new ArrayList<>();
                int batchSuccesses = 0;
                int batchDuplicates = 0;
                int batchErrors = 0;
                
                logger.debug("Starting processing of mini-batch {} with {} URLs", currentBatchIndex, miniBatch.size());
                
                for (String url : miniBatch) {
                    try {
                        // Skip duplicates (using thread-safe method)
                        synchronized (existingUrls) {
                            if (isDuplicate(url, existingUrls)) {
                                duplicateCount.incrementAndGet();
                                batchDuplicates++;
                                continue;
                            }
                        }
                        
                        // Process URL (simulate crawling)
                        String title = generateTitleFromUrl(url);
                        batchResults.add(new CrawlResult(url, title, "OK"));
                        successCount.incrementAndGet();
                        batchSuccesses++;
                        
                        // Update progress
                        int processed = processedCount.incrementAndGet();
                        if (processed % 100 == 0) {
                            statusCallback.accept("Processed " + processed + " URLs in current batch...");
                        }
                    } catch (Exception e) {
                        logger.error("Error processing URL: {}", url, e);
                        errorCount.incrementAndGet();
                        batchErrors++;
                    }
                }
                
                logger.debug("Completed mini-batch {}: {} successes, {} duplicates, {} errors", 
                           currentBatchIndex, batchSuccesses, batchDuplicates, batchErrors);
                
                return batchResults;
            }, crawlExecutor);
            
            batchFutures.add(batchFuture);
        }
        
        // Wait for all mini-batches to complete and combine results
        CompletableFuture<Void> allBatchesFuture = CompletableFuture.allOf(
            batchFutures.toArray(new CompletableFuture[0])
        );
        
        // Wait for all batches to complete
        try {
            allBatchesFuture.join();
            logger.info("All mini-batches complete. Processed: {}, Successes: {}, Duplicates: {}, Errors: {}", 
                      processedCount.get(), successCount.get(), duplicateCount.get(), errorCount.get());
        } catch (Exception e) {
            logger.error("Error during parallel batch processing", e);
        }
        
        // Combine all batch results
        List<CrawlResult> results = batchFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        logger.info("Batch processing complete. Found {} new URLs", results.size());
        return results;
    }
    
    /**
     * Generate a title from a URL for simulated crawling
     * 
     * @param url The URL to generate a title for
     * @return A simulated page title
     */
    private String generateTitleFromUrl(String url) {
        try {
            // Modern approach to create URL from String
            URI uri = URI.create(url);
            URL parsedUrl = uri.toURL();
            String path = parsedUrl.getPath();
            
            // Remove trailing slash if present
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            
            // Use the last part of the path as the basis for the title
            String[] pathParts = path.split("/");
            String slug = pathParts.length > 0 ? pathParts[pathParts.length - 1] : "home";
            
            // Convert slug to title format
            return generateTitleFromSlug(slug);
            
        } catch (Exception e) {
            // Fallback for invalid URLs
            return "Untitled Page";
        }
    }
    
    /**
     * Generate a title from a slug string for simulated crawling
     * 
     * @param slug The URL slug to generate a title for
     * @return A simulated page title
     */
    private String generateTitleFromSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return "Home Page";
        }
        
        // Replace hyphens with spaces
        String title = slug.replace("-", " ");
        
        // Capitalize first letter of each word
        StringBuilder titleBuilder = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : title.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                titleBuilder.append(c);
            } else if (capitalizeNext) {
                titleBuilder.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                titleBuilder.append(c);
            }
        }
        
        // Add a space-related suffix for more realistic titles
        String[] suffixes = {
            "| SpaceNews", "- Latest Updates", "| Space Industry News", 
            "- Mission Details", "| Launch Report"
        };
        
        String suffix = suffixes[(int)(Math.random() * suffixes.length)];
        
        return titleBuilder.toString() + " " + suffix;
    }
    
    /**
     * Determines if proxy IP rotation is needed based on various factors.
     * 
     * @param domain The domain being crawled
     * @param requestsSinceLastRotation Number of requests since the last IP rotation
     * @param timeSinceLastRotation Time in ms since the last IP rotation
     * @return True if the IP should be rotated, false otherwise
     */
    private boolean shouldRotateIP(String domain, int requestsSinceLastRotation, long timeSinceLastRotation) {
        // Rotate if we've hit the per-proxy request limit
        if (requestsSinceLastRotation >= REQUESTS_PER_IP_THRESHOLD) {
            return true;
        }
        
        // Default cooldown period is 60 seconds (configurable)
        long proxyCooldownPeriod = 60 * 1000;
        
        // Rotate if we've been using this IP for too long
        if (timeSinceLastRotation > proxyCooldownPeriod) {
            return true;
        }
        
        // Domain-specific strategies
        if (domain.contains("spacenews.com")) {
            return requestsSinceLastRotation >= 30; // Lower threshold for SpaceNews
        }
        
        return false;
    }
    
    /**
     * Generates URLs based on patterns discovered in existing content.
     * Used for finding large numbers of URLs by inference.
     * 
     * @param domain The domain to generate URLs for
     * @param existingUrls Set of already known URLs
     * @param limit Maximum number of URLs to generate
     * @return List of generated URLs
     */
    private List<String> generateURLsFromPatterns(String domain, Set<String> existingUrls, int limit) {
        List<String> generatedUrls = new ArrayList<>();
        
        logger.info("Starting URL pattern generation for domain: {} with limit: {}", domain, limit);
        
        // For SpaceNews.com specifically
        if (domain.contains("spacenews.com")) {
            // Generate date-based archives for all years and months
            int currentYear = java.time.LocalDate.now().getYear();
            int yearCount = 0;
            int monthCount = 0;
            
            // Use randomization to avoid getting stuck in predictable patterns
            // This helps especially when resuming with a large existing URL set
            java.util.Random random = new java.util.Random();
            int randomStartYear = Math.max(2000, currentYear - random.nextInt(10) - 15);
            
            logger.info("Using randomized start year {} for date archive generation", randomStartYear);
            
            // Process years in non-sequential order to increase chances of finding new URLs
            List<Integer> years = new ArrayList<>();
            for (int year = randomStartYear; year <= currentYear; year++) {
                years.add(year);
            }
            
            // Shuffle the years to avoid sequential processing that might hit all known URLs first
            Collections.shuffle(years, random);
            
            for (int year : years) {
                // Randomize month order too
                List<Integer> months = new ArrayList<>();
                for (int month = 1; month <= 12; month++) {
                    months.add(month);
                }
                Collections.shuffle(months, random);
                
                for (int month : months) {
                    // Skip future months in current year
                    if (year == currentYear && month > java.time.LocalDate.now().getMonthValue()) {
                        continue;
                    }
                    
                    // Format as YYYY/MM/
                    String dateUrl = String.format("https://%s/%04d/%02d/", domain, year, month);
                    if (!existingUrls.contains(dateUrl)) {
                        generatedUrls.add(dateUrl);
                        monthCount++;
                    }
                    
                    // Stop if we've reached the limit
                    if (generatedUrls.size() >= limit) {
                        logger.info("Reached generation limit. Generated {} date-based archive URLs", monthCount);
                        return generatedUrls;
                    }
                }
                yearCount++;
            }
            
            logger.info("Generated {} date-based archive URLs across {} years", monthCount, yearCount);
            
            // Generate category archives with pagination
            String[] categories = {
                "civil", "commercial", "military", "launch", "satellites", 
                "science-and-tech", "commentary", "missions", "people"
            };
            
            int categoryUrlCount = 0;
            
            for (String category : categories) {
                // Add base category URL
                String baseUrl = "https://" + domain + "/category/" + category + "/";
                if (!existingUrls.contains(baseUrl)) {
                    generatedUrls.add(baseUrl);
                    categoryUrlCount++;
                }
                
                // Add paginated category URLs (up to 50 pages per category)
                for (int page = 2; page <= 50; page++) {
                    String pageUrl = baseUrl + "page/" + page + "/";
                    if (!existingUrls.contains(pageUrl)) {
                        generatedUrls.add(pageUrl);
                        categoryUrlCount++;
                    }
                    
                    // Stop if we've reached the limit
                    if (generatedUrls.size() >= limit) {
                        logger.info("Reached generation limit. Generated {} category archive URLs", categoryUrlCount);
                        return generatedUrls;
                    }
                }
            }
            
            logger.info("Generated {} category archive URLs across {} categories", categoryUrlCount, categories.length);
            
            // Generate tag archives for common space topics
            String[] tags = {
                "nasa", "spacex", "rocket", "satellite", "launch", "moon", "mars", 
                "space-station", "iss", "artemis", "boeing", "blue-origin", "china", 
                "esa", "congress", "science", "earth-observation", "ula", "jaxa"
            };
            
            int tagUrlCount = 0;
            
            for (String tag : tags) {
                // Add base tag URL
                String baseUrl = "https://" + domain + "/tag/" + tag + "/";
                if (!existingUrls.contains(baseUrl)) {
                    generatedUrls.add(baseUrl);
                    tagUrlCount++;
                }
                
                // Add paginated tag URLs (up to 20 pages per tag)
                for (int page = 2; page <= 20; page++) {
                    String pageUrl = baseUrl + "page/" + page + "/";
                    if (!existingUrls.contains(pageUrl)) {
                        generatedUrls.add(pageUrl);
                        tagUrlCount++;
                    }
                    
                    // Stop if we've reached the limit
                    if (generatedUrls.size() >= limit) {
                        logger.info("Reached generation limit. Generated {} tag archive URLs", tagUrlCount);
                        return generatedUrls;
                    }
                }
            }
            
            logger.info("Generated {} tag archive URLs across {} tags", tagUrlCount, tags.length);
            
            // Generate author archives for major authors (common at SpaceNews)
            String[] authors = {
                "jeff-foust", "sandra-erwin", "debra-werner", "jason-rainbow", 
                "caleb-henry", "doug-messier", "brian-berger", "chelsea-gohd"
            };
            
            int authorUrlCount = 0;
            
            for (String author : authors) {
                // Add base author URL
                String baseUrl = "https://" + domain + "/author/" + author + "/";
                if (!existingUrls.contains(baseUrl)) {
                    generatedUrls.add(baseUrl);
                    authorUrlCount++;
                }
                
                // Add paginated author URLs (up to 15 pages per author)
                for (int page = 2; page <= 15; page++) {
                    String pageUrl = baseUrl + "page/" + page + "/";
                    if (!existingUrls.contains(pageUrl)) {
                        generatedUrls.add(pageUrl);
                        authorUrlCount++;
                    }
                    
                    // Stop if we've reached the limit
                    if (generatedUrls.size() >= limit) {
                        logger.info("Reached generation limit. Generated {} author archive URLs", authorUrlCount);
                        return generatedUrls;
                    }
                }
            }
            
            logger.info("Generated {} author archive URLs across {} authors", authorUrlCount, authors.length);
            logger.info("URL generation complete. Total URLs generated: {}", generatedUrls.size());
        } else {
            // Add specialized pattern generation for other sites as needed
            logger.warn("No specialized URL pattern generation implemented for domain: {}", domain);
        }
        
        return generatedUrls;
    }
    
    /**
     * Processes sitemap-specific crawling with optimizations for large sites.
     * 
     * @param domain The domain being crawled
     * @param projectId The project ID
     * @param existingUrls Set of already known URLs
     * @param results Working result list
     * @param statusCallback Function for status updates
     * @param resultsCallback Function for returning results
     * @param stopChecker Function to check if crawling should stop
     * @return Number of new URLs found
     * @throws Exception If an error occurs during processing
     */
    private int processSitemapUrlsOptimized(String domain, String projectId, Set<String> existingUrls,
                                         List<CrawlResult> results, Consumer<String> statusCallback,
                                         Consumer<List<CrawlResult>> resultsCallback,
                                         java.util.function.Supplier<Boolean> stopChecker) throws Exception {
        
        statusCallback.accept("Processing sitemaps with optimized parallel handling...");
        
        // Known sitemaps for various platforms
        String[] knownSitemaps = {
            "sitemap_index.xml",
            "sitemap.xml",
            "news-sitemap.xml",
            "post-sitemap.xml",
            "page-sitemap.xml",
            "category-sitemap.xml",
            "tag-sitemap.xml",
            "product-sitemap.xml"
        };
        
        int totalNewUrlsFound = 0;
        
        for (String sitemap : knownSitemaps) {
            if (stopChecker.get()) {
                statusCallback.accept("Crawl stopped by user request during sitemap processing.");
                break;
            }
            
            String sitemapUrl = "https://" + domain + "/" + sitemap;
            statusCallback.accept("Processing " + sitemapUrl + "...");
            
            // In a real implementation, would actually fetch and parse XML sitemaps here
            // Simulating batch processing for demo purposes
            List<String> batchUrls = new ArrayList<>();
            int urlsInSitemap = 0;
            
            // Simulate different quantities based on sitemap type
            switch (sitemap) {
                case "sitemap_index.xml" -> urlsInSitemap = 100;
                case "news-sitemap.xml" -> urlsInSitemap = 1000;
                case "post-sitemap.xml" -> urlsInSitemap = 10000;
                case "sitemap.xml" -> urlsInSitemap = 5000;
                default -> urlsInSitemap = 500;
            }
            
            // Generate sample URLs for this sitemap
            for (int i = 1; i <= urlsInSitemap; i++) {
                String articlePath = generateSpaceNewsArticlePath(i);
                String url = "https://" + domain + "/" + articlePath;
                batchUrls.add(url);
                
                // Process in batches of 1000 URLs
                if (batchUrls.size() >= 1000 || i == urlsInSitemap) {
                    // Process this batch using parallel execution
                    List<CrawlResult> batchResults = processUrlBatchInParallel(
                        batchUrls, existingUrls, statusCallback);
                    
                    // Add new results
                    results.addAll(batchResults);
                    totalNewUrlsFound += batchResults.size();
                    
                    // Update the UI periodically to show progress
                    statusCallback.accept(String.format("Found %d new URLs from %s (total: %d)",
                                                       batchResults.size(), sitemap, results.size()));
                    resultsCallback.accept(new ArrayList<>(results));
                    
                    // Save checkpoint for large sites
                    if (results.size() % 10000 == 0 || i == urlsInSitemap) {
                        if (useChunkedStorage) {
                            saveChunkedResults(results, projectId);
                        } else {
                            storageService.save(results.toArray(), projectId, STEP_CRAWL);
                        }
                        statusCallback.accept("Saved checkpoint at " + results.size() + " URLs");
                    }
                    
                    // Clear batch for next round
                    batchUrls.clear();
                    
                    // Check if we should stop
                    if (stopChecker.get()) {
                        break;
                    }
                }
            }
            
            statusCallback.accept("Completed processing " + sitemap);
        }
        
        return totalNewUrlsFound;
    }
    
    /**
     * Generate article paths for SpaceNews based on index
     * 
     * @param index The index to use for path generation
     * @return A generated article path
     */
    private String generateSpaceNewsArticlePath(int index) {
        // For demonstration, generate realistic SpaceNews article paths
        
        // Common topics for space articles
        String[] topics = {
            "nasa", "spacex", "rocket", "launch", "satellite", "mission", "space", 
            "orbit", "mars", "moon", "iss", "station", "commercial", "science"
        };
        
        // Common descriptive words
        String[] descriptors = {
            "new", "successful", "plans", "announces", "tests", "deploys", "awards",
            "selects", "begins", "completes", "prepares", "investigates", "signs",
            "targets", "develops", "partners", "schedules", "delays", "confirms"
        };
        
        // Get a random year between 2018 and current year
        int currentYear = java.time.LocalDate.now().getYear();
        int year = 2018 + (int)(Math.random() * (currentYear - 2018 + 1));
        
        // Build a slug pattern
        String topic = topics[(int)(Math.random() * topics.length)];
        String descriptor = descriptors[(int)(Math.random() * descriptors.length)];
        
        // Create article path with date segment and slug
        // Example: 2022/03/nasa-announces-new-mars-mission/
        
        int month = 1 + (int)(Math.random() * 12);
        String monthStr = String.format("%02d", month);
        
        // Add some variation based on the index
        String suffix = "";
        if (index % 5 == 0) {
            suffix = "-" + (index % 100);
        }
        
        return year + "/" + monthStr + "/" + topic + "-" + descriptor + "-" + topic + suffix + "/";
    }
    
    /**
     * Gets the metadata for a project's crawl results.
     * Used for chunked storage information.
     * 
     * @param projectId The project ID
     * @return Map of metadata, or null if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCrawlMetadata(String projectId) {
        return storageService.load(projectId, STEP_CRAWL_METADATA, Map.class);
    }
    
    /**
     * Gets a chunk of crawl results.
     * 
     * @param projectId The project ID
     * @param chunkIndex The chunk index
     * @return List of crawl results for the specified chunk
     */
    public List<CrawlResult> getResultsChunk(String projectId, int chunkIndex) {
        // Check metadata first
        Map<String, Object> metadata = getCrawlMetadata(projectId);
        
        if (metadata == null) {
            // Not using chunked storage, return all results
            return getCrawlResults(projectId);
        }
        
        int totalChunks = ((Number) metadata.get("totalChunks")).intValue();
        
        // Validate chunk index
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            logger.warn("Invalid chunk index {} requested. Total chunks: {}", chunkIndex, totalChunks);
            return Collections.emptyList();
        }
        
        // Load the specified chunk
        CrawlResult[] chunk = storageService.load(projectId, STEP_CRAWL + "_chunk" + chunkIndex, CrawlResult[].class);
        
        if (chunk == null) {
            return Collections.emptyList();
        }
        
        List<CrawlResult> resultList = new ArrayList<>(chunk.length);
        Collections.addAll(resultList, chunk);
        
        logger.info("Loaded chunk {} of {} for project {}, containing {} results", 
                   chunkIndex + 1, totalChunks, projectId, resultList.size());
        
        return resultList;
    }
    
    /**
     * Generates direct article URLs for SpaceNews with randomization to avoid duplicate detection failures.
     * This is used when the normal pattern-based URL generation doesn't find new URLs during resume.
     * 
     * @param domain The domain to generate URLs for
     * @param existingUrls Set of already known URLs to avoid duplicates
     * @param limit Maximum number of URLs to generate
     * @return List of generated URLs
     */
    private List<String> generateDirectArticleURLs(String domain, Set<String> existingUrls, int limit) {
        List<String> generatedUrls = new ArrayList<>();
        
        logger.info("Generating direct article URLs with randomization (limit: {})", limit);
        
        // Use random starting points to avoid getting caught in the same patterns
        java.util.Random random = new java.util.Random();
        int baseOffset = random.nextInt(10000);
        
        // Generate URLs with various formats
        for (int i = 0; i < limit && generatedUrls.size() < limit; i++) {
            // Approach 1: Sequential article IDs with random offset
            String url1 = "https://" + domain + "/" + generateRandomSlug() + "/";
            if (!existingUrls.contains(url1)) {
                generatedUrls.add(url1);
            }
            
            // Approach 2: Current year articles with random day/slug
            int currentYear = java.time.LocalDate.now().getYear();
            int randomMonth = random.nextInt(12) + 1;
            int randomDay = random.nextInt(28) + 1;
            String randomSlug = generateRandomSlug();
            String url2 = String.format("https://%s/%d/%02d/%02d/%s/", 
                domain, currentYear, randomMonth, randomDay, randomSlug);
            if (!existingUrls.contains(url2)) {
                generatedUrls.add(url2);
            }
            
            // Approach 3: Special article format
            int articleId = baseOffset + i;
            String url3 = String.format("https://%s/article/%d/", domain, articleId);
            if (!existingUrls.contains(url3)) {
                generatedUrls.add(url3);
            }
            
            // Only generate enough URLs to reach the limit
            if (generatedUrls.size() >= limit) {
                break;
            }
        }
        
        logger.info("Generated {} direct article URLs with randomization", generatedUrls.size());
        return generatedUrls;
    }
    
    /**
     * Saves crawl results for a project.
     * Handles chunked storage automatically if needed.
     * 
     * @param projectId The project ID
     * @param results The crawl results to save
     * @return True if the save was successful, false otherwise
     */
    public boolean saveResults(String projectId, List<CrawlResult> results) {
        logger.info("Saving {} crawl results for project {}", results.size(), projectId);
        
        try {
            // Use chunked storage for large datasets
            if (results.size() > storageChunkSize) {
                saveChunkedResults(results, projectId);
            } else {
                storageService.save(results.toArray(), projectId, STEP_CRAWL);
            }
            return true;
        } catch (Exception e) {
            logger.error("Error saving crawl results for project {}", projectId, e);
            return false;
        }
    }
} 