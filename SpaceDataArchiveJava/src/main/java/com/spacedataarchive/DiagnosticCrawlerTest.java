package com.spacedataarchive;

import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.service.SiteMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Diagnostic tool to test crawler without the JavaFX UI.
 * This allows direct testing of the URL generation and crawling functionality.
 */
public class DiagnosticCrawlerTest {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticCrawlerTest.class);
    
    public static void main(String[] args) {
        logger.info("Starting crawler diagnostic test");
        
        SiteMapService siteMapService = SiteMapService.getInstance();
        
        // Create a test project
        String projectName = "diagnostic-test-" + System.currentTimeMillis();
        String seedUrl = "https://spacenews.com";
        SiteMapProject project = siteMapService.createProject(projectName, seedUrl, 3, 100000);
        
        // The actual project ID is stored in the project object
        String projectId = project.getId();
        logger.info("Created test project with name: {}, assigned ID: {}", projectName, projectId);
        
        // Start a crawl with console status updates
        CompletableFuture<List<CrawlResult>> crawlFuture = siteMapService.startCrawl(
            projectId,
            status -> logger.info("Crawler status: {}", status),
            results -> {
                if (results.size() % 100 == 0) {
                    logger.info("Current results count: {}", results.size());
                }
            }
        );
        
        // Wait for completion
        try {
            List<CrawlResult> results = crawlFuture.get();
            logger.info("Crawl completed. Found {} URLs", results.size());
            
            // Log a sample of the results
            logger.info("Sample of discovered URLs:");
            results.stream()
                .limit(20)
                .forEach(result -> logger.info("URL: {}, Title: {}", result.getUrl(), result.getTitle()));
            
        } catch (Exception e) {
            logger.error("Error during diagnostic crawl", e);
        }
        
        logger.info("Diagnostic test complete");
        System.exit(0); // Force exit since crawler may have background threads
    }
} 