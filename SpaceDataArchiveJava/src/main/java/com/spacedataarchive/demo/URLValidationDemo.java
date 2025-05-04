package com.spacedataarchive.demo;

import com.spacedataarchive.model.EnhancedValidationResult;
import com.spacedataarchive.model.ValidatedUrlCollection;
import com.spacedataarchive.service.ServiceFactory;
import com.spacedataarchive.service.URLValidationService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Demonstration of the URL validation functionality.
 */
public class URLValidationDemo {

    /**
     * Main method to run the demonstration.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        System.out.println("URL Validation Demo");
        System.out.println("===================\n");
        
        // Get the validation service
        URLValidationService validationService = ServiceFactory.getInstance().getUrlValidationService();
        
        // Demo of simple URL validation
        demonstrateSimpleValidation(validationService);
        
        // Demo of AI-assisted URL validation
        demonstrateAIValidation(validationService);
        
        // Demo of site map integration
        demonstrateSiteMapIntegration(validationService);
        
        // Shutdown services
        ServiceFactory.getInstance().shutdown();
        
        System.out.println("\nDemo complete!");
    }
    
    /**
     * Demonstrates simple URL validation.
     * 
     * @param validationService The validation service
     */
    private static void demonstrateSimpleValidation(URLValidationService validationService) {
        System.out.println("\n1. Simple URL Validation");
        System.out.println("----------------------");
        
        // Valid URL
        String validUrl = "https://www.nasa.gov";
        System.out.println("\nValidating URL: " + validUrl);
        System.out.println(validationService.validateUrl(validUrl));
        
        // Invalid URL
        String invalidUrl = "http://invalid-domain-xyz-123.com";
        System.out.println("\nValidating URL: " + invalidUrl);
        System.out.println(validationService.validateUrl(invalidUrl));
        
        // Malformed URL
        String malformedUrl = "not a url";
        System.out.println("\nValidating URL: " + malformedUrl);
        System.out.println(validationService.validateUrl(malformedUrl));
    }
    
    /**
     * Demonstrates AI-assisted URL validation.
     * 
     * @param validationService The validation service
     */
    private static void demonstrateAIValidation(URLValidationService validationService) {
        System.out.println("\n2. AI-Assisted URL Validation");
        System.out.println("---------------------------");
        
        String url = "https://www.nasa.gov/missions/";
        System.out.println("\nValidating URL with AI: " + url);
        
        EnhancedValidationResult result = validationService.validateUrlWithAI(url);
        System.out.println("Result: " + result);
        System.out.println("Asset Type: " + result.getAssetType());
        System.out.println("URL Name: " + result.getUrlName());
        System.out.println("Content Type: " + result.getContentType());
        System.out.println("Metadata: " + result.getMetadata());
    }
    
    /**
     * Demonstrates site map integration.
     * 
     * @param validationService The validation service
     */
    private static void demonstrateSiteMapIntegration(URLValidationService validationService) {
        System.out.println("\n3. Site Map Integration");
        System.out.println("----------------------");
        
        String siteMapId = "demo-sitemap";
        
        // Create a validation collection from a site map
        System.out.println("\nCreating validation collection from site map: " + siteMapId);
        ValidatedUrlCollection collection = validationService.createValidationFromSiteMap(siteMapId, "Demo Validation");
        System.out.println("Created collection: " + collection.getId());
        
        // Start validation
        System.out.println("\nStarting validation process...");
        try {
            CompletableFuture<ValidatedUrlCollection> future = validationService.startValidation(
                collection.getId(), 3, true);
            
            // Monitor progress
            while (!future.isDone()) {
                Map<String, Object> status = validationService.getValidationStatus(collection.getId());
                System.out.println("Status: " + status.get("state") + ", Progress: " + status.get("progress") + "%, Processed: " + status.get("processed"));
                Thread.sleep(500);
            }
            
            // Get the completed result
            ValidatedUrlCollection result = future.get();
            System.out.println("\nValidation completed!");
            System.out.println("Statistics: " + result.getStatistics());
            System.out.println("Validated URLs: " + result.getUrls().size());
            
            // Print first 3 results
            List<EnhancedValidationResult> urls = result.getUrls();
            for (int i = 0; i < Math.min(3, urls.size()); i++) {
                EnhancedValidationResult url = urls.get(i);
                System.out.println("\nURL #" + (i + 1) + ": " + url.getUrl());
                System.out.println("Status: " + url.getStatus());
                System.out.println("Asset Type: " + url.getAssetType());
                System.out.println("URL Name: " + url.getUrlName());
            }
            
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error during validation: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 