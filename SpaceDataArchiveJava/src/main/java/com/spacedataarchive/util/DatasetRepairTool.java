package com.spacedataarchive.util;

import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.service.SiteMapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility tool for checking and repairing datasets that may have duplicate URLs.
 * Can be used to verify data integrity and fix issues with duplicate entries.
 */
public class DatasetRepairTool {
    private static final Logger logger = LoggerFactory.getLogger(DatasetRepairTool.class);
    
    private final SiteMapService siteMapService;
    
    /**
     * Creates a new DatasetRepairTool.
     */
    public DatasetRepairTool() {
        this.siteMapService = SiteMapService.getInstance();
    }
    
    /**
     * Main method for running the repair tool from the command line.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        DatasetRepairTool tool = new DatasetRepairTool();
        
        // List available projects
        List<SiteMapProject> projects = tool.listProjects();
        
        if (projects.isEmpty()) {
            System.out.println("No projects found. Nothing to repair.");
            return;
        }
        
        System.out.println("Available projects:");
        for (int i = 0; i < projects.size(); i++) {
            SiteMapProject project = projects.get(i);
            System.out.println((i + 1) + ". " + project.getName() + " (ID: " + project.getId() + ")");
        }
        
        // Allow user to select a project
        try (Scanner scanner = new Scanner(System.in)) {
            int selection = -1;
            
            while (selection < 1 || selection > projects.size()) {
                System.out.print("\nEnter project number to repair (1-" + projects.size() + "): ");
                try {
                    selection = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                }
            }
            
            SiteMapProject selectedProject = projects.get(selection - 1);
            System.out.println("\nSelected project: " + selectedProject.getName());
            
            // Analyze the project
            DuplicateAnalysisResult analysis = tool.analyzeDuplicates(selectedProject.getId());
            
            if (analysis.getTotalDuplicates() == 0) {
                System.out.println("\nNo duplicates found. Dataset is clean.");
                return;
            }
            
            // Ask if the user wants to repair the dataset
            System.out.print("\nRepair dataset by removing duplicates? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            
            if (response.equals("y") || response.equals("yes")) {
                boolean success = tool.repairDataset(selectedProject.getId());
                
                if (success) {
                    System.out.println("\nDataset repaired successfully.");
                    
                    // Verify the repair
                    DuplicateAnalysisResult verificationAnalysis = tool.analyzeDuplicates(selectedProject.getId());
                    System.out.println("\nVerification after repair:");
                    System.out.println("Total URLs: " + verificationAnalysis.getTotalUrls());
                    System.out.println("Unique URLs: " + verificationAnalysis.getUniqueUrls());
                    System.out.println("Duplicate entries: " + verificationAnalysis.getTotalDuplicates());
                } else {
                    System.out.println("\nFailed to repair dataset. See logs for details.");
                }
            } else {
                System.out.println("\nRepair cancelled.");
            }
        }
    }
    
    /**
     * Lists all available projects.
     * 
     * @return A list of projects
     */
    public List<SiteMapProject> listProjects() {
        return siteMapService.listProjects();
    }
    
    /**
     * Analyzes a project's dataset for duplicate URLs.
     * 
     * @param projectId The project ID
     * @return Analysis result with duplicate statistics
     */
    public DuplicateAnalysisResult analyzeDuplicates(String projectId) {
        logger.info("Analyzing project {} for duplicates", projectId);
        System.out.println("\nAnalyzing dataset for duplicates...");
        
        List<CrawlResult> results = siteMapService.getCrawlResults(projectId);
        
        if (results.isEmpty()) {
            logger.info("Project {} has no crawl results", projectId);
            System.out.println("No crawl results found for this project.");
            return new DuplicateAnalysisResult(0, 0, 0, Collections.emptyMap());
        }
        
        long startTime = System.currentTimeMillis();
        
        // Find duplicates
        Set<String> uniqueUrls = new HashSet<>();
        Map<String, Integer> duplicateCount = new HashMap<>();
        
        for (CrawlResult result : results) {
            String url = result.getUrl();
            if (!uniqueUrls.add(url)) {
                // URL already exists in set, it's a duplicate
                duplicateCount.put(url, duplicateCount.getOrDefault(url, 1) + 1);
            }
        }
        
        int totalDuplicates = duplicateCount.values().stream().mapToInt(Integer::intValue).sum();
        
        long duration = System.currentTimeMillis() - startTime;
        
        DuplicateAnalysisResult analysis = new DuplicateAnalysisResult(
            results.size(),
            uniqueUrls.size(),
            totalDuplicates,
            duplicateCount
        );
        
        // Print analysis results
        System.out.println("\nAnalysis completed in " + duration + "ms");
        System.out.println("Total URLs: " + analysis.getTotalUrls());
        System.out.println("Unique URLs: " + analysis.getUniqueUrls());
        System.out.println("URLs with duplicates: " + duplicateCount.size());
        System.out.println("Total duplicate entries: " + totalDuplicates);
        
        if (totalDuplicates > 0) {
            System.out.println("\nTop duplicates:");
            duplicateCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> System.out.println("- " + entry.getKey() + " (" + entry.getValue() + " occurrences)"));
        }
        
        return analysis;
    }
    
    /**
     * Repairs a project's dataset by removing duplicate URLs.
     * 
     * @param projectId The project ID
     * @return True if the repair was successful, false otherwise
     */
    public boolean repairDataset(String projectId) {
        logger.info("Repairing project {} by removing duplicate URLs", projectId);
        System.out.println("\nRepairing dataset...");
        
        try {
            List<CrawlResult> results = siteMapService.getCrawlResults(projectId);
            
            if (results.isEmpty()) {
                logger.info("Project {} has no crawl results to repair", projectId);
                return true;
            }
            
            // Deduplicate by using a LinkedHashMap to preserve insert order
            Map<String, CrawlResult> uniqueResults = new LinkedHashMap<>();
            for (CrawlResult result : results) {
                uniqueResults.putIfAbsent(result.getUrl(), result);
            }
            
            List<CrawlResult> dedupedResults = new ArrayList<>(uniqueResults.values());
            
            int removedCount = results.size() - dedupedResults.size();
            logger.info("Removed {} duplicate entries from project {}", removedCount, projectId);
            System.out.println("Removed " + removedCount + " duplicate entries");
            
            // Save the deduplicated results back to storage
            if (siteMapService.saveResults(projectId, dedupedResults)) {
                logger.info("Successfully saved deduplicated results for project {}", projectId);
                return true;
            } else {
                logger.error("Failed to save deduplicated results for project {}", projectId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error repairing dataset for project {}", projectId, e);
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Class to hold the results of a duplicate analysis.
     */
    public static class DuplicateAnalysisResult {
        private final int totalUrls;
        private final int uniqueUrls;
        private final int totalDuplicates;
        private final Map<String, Integer> duplicateCounts;
        
        public DuplicateAnalysisResult(int totalUrls, int uniqueUrls, int totalDuplicates, 
                                        Map<String, Integer> duplicateCounts) {
            this.totalUrls = totalUrls;
            this.uniqueUrls = uniqueUrls;
            this.totalDuplicates = totalDuplicates;
            this.duplicateCounts = duplicateCounts;
        }
        
        public int getTotalUrls() {
            return totalUrls;
        }
        
        public int getUniqueUrls() {
            return uniqueUrls;
        }
        
        public int getTotalDuplicates() {
            return totalDuplicates;
        }
        
        public Map<String, Integer> getDuplicateCounts() {
            return duplicateCounts;
        }
    }
} 