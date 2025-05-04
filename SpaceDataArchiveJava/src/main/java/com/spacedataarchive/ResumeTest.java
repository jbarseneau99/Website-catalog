package com.spacedataarchive;

import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.service.SiteMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Test class to specifically test resume crawling functionality.
 * This allows direct testing of the resume feature without the JavaFX UI.
 */
public class ResumeTest {
    private static final Logger logger = LoggerFactory.getLogger(ResumeTest.class);
    
    public static void main(String[] args) {
        logger.info("Starting resume crawler test");
        
        SiteMapService siteMapService = SiteMapService.getInstance();
        
        // Get first project from database to resume
        List<SiteMapProject> projects = siteMapService.listProjects();
        
        if (projects.isEmpty()) {
            logger.error("No projects found to resume. Please create a project first.");
            System.exit(1);
        }
        
        SiteMapProject projectToResume = projects.get(0);
        String projectId = projectToResume.getId();
        
        logger.info("Found project to resume: {} (ID: {})", projectToResume.getName(), projectId);
        
        // Load and analyze the existing results before resuming
        List<CrawlResult> existingResults = siteMapService.getCrawlResults(projectId);
        int currentUrlCount = existingResults.size();
        
        System.out.println("=====================================");
        System.out.println("RESUME CRAWLER TEST");
        System.out.println("=====================================");
        System.out.println("Project: " + projectToResume.getName());
        System.out.println("Current URLs: " + currentUrlCount);
        
        // Check for duplicates in existing results
        checkDuplicates(existingResults);
        
        System.out.println("=====================================");
        
        // Confirm before starting
        System.out.println("Press ENTER to start resuming crawl for project: " + projectToResume.getName());
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }
        
        System.out.println("Starting resume crawl...");
        
        // Start a crawl with console status updates
        CompletableFuture<List<CrawlResult>> crawlFuture = siteMapService.startCrawl(
            projectId,
            status -> {
                logger.info("Crawler status: {}", status);
                System.out.println("[STATUS] " + status);
            },
            results -> {
                int newCount = results.size() - currentUrlCount;
                if (results.size() % 100 == 0 || newCount > 0) {
                    logger.info("Current results count: {} (new: {})", results.size(), newCount);
                    System.out.println("[COUNT] Found " + results.size() + " URLs (+" + newCount + " new)");
                }
            }
        );
        
        // Wait for completion
        try {
            List<CrawlResult> results = crawlFuture.get();
            int newUrlsFound = results.size() - currentUrlCount;
            
            logger.info("Crawl completed. Total URLs: {}, New URLs: {}", results.size(), newUrlsFound);
            
            System.out.println("=====================================");
            System.out.println("RESUME TEST COMPLETE");
            System.out.println("=====================================");
            System.out.println("Total URLs: " + results.size());
            System.out.println("New URLs found: " + newUrlsFound);
            
            // Check for duplicates in final results
            checkDuplicates(results);
            
            if (newUrlsFound > 0) {
                System.out.println("\nSuccess! Resume crawl found new URLs");
                
                // Log the most recent URLs found
                System.out.println("\nSample of newly discovered URLs:");
                results.stream()
                    .skip(Math.max(0, results.size() - 20))
                    .limit(20)
                    .forEach(result -> System.out.println("- " + result.getUrl()));
            } else {
                System.out.println("Failed! Resume crawl did not find any new URLs");
            }
            System.out.println("=====================================");
            
        } catch (Exception e) {
            logger.error("Error during resume crawl", e);
            e.printStackTrace();
        }
        
        logger.info("Resume test complete");
        System.exit(0); // Force exit since crawler may have background threads
    }
    
    /**
     * Checks for duplicate URLs in the results list and logs statistics.
     * 
     * @param results The list of crawl results to check
     */
    private static void checkDuplicates(List<CrawlResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("No results to check for duplicates.");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        // Use a Set to find duplicates
        Set<String> uniqueUrls = new HashSet<>();
        Map<String, Integer> duplicateCount = new HashMap<>();
        
        for (CrawlResult result : results) {
            String url = result.getUrl();
            if (!uniqueUrls.add(url)) {
                // URL already exists in set, it's a duplicate
                duplicateCount.put(url, duplicateCount.getOrDefault(url, 1) + 1);
            }
        }
        
        int duplicateUrls = duplicateCount.size();
        int totalDuplicates = duplicateCount.values().stream().mapToInt(Integer::intValue).sum();
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("\nDuplicate analysis completed in " + (endTime - startTime) + "ms");
        System.out.println("Total URLs: " + results.size());
        System.out.println("Unique URLs: " + uniqueUrls.size());
        System.out.println("URLs with duplicates: " + duplicateUrls);
        System.out.println("Total duplicate entries: " + totalDuplicates);
        
        if (duplicateUrls > 0) {
            System.out.println("\nExample duplicates:");
            duplicateCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> System.out.println("- " + entry.getKey() + " (" + entry.getValue() + " occurrences)"));
        }
    }
} 