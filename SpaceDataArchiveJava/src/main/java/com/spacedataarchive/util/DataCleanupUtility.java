package com.spacedataarchive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utility for cleaning up large data files to improve application startup time.
 */
public class DataCleanupUtility {
    private static final Logger logger = LoggerFactory.getLogger(DataCleanupUtility.class);
    private static final String DATA_DIR = "data";
    private static final long MAX_FILE_SIZE_MB = 10; // Maximum file size in MB
    private static final int MAX_FILES_PER_TYPE = 5; // Maximum number of files per type to keep

    /**
     * Cleans up large validation files.
     */
    public static void cleanupLargeDataFiles() {
        logger.info("Starting data cleanup to improve application performance");
        
        try {
            File dataDir = new File(DATA_DIR);
            if (!dataDir.exists() || !dataDir.isDirectory()) {
                logger.info("Data directory not found, no cleanup needed");
                return;
            }
            
            // Cleanup validation files
            cleanupValidationFiles(dataDir);
            
            // Cleanup large crawl.json files (use chunked data instead)
            cleanupLargeCrawlFiles(dataDir);
            
            logger.info("Data cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during data cleanup", e);
        }
    }
    
    /**
     * Cleans up large validation files, keeping only recent ones.
     */
    private static void cleanupValidationFiles(File dataDir) {
        try {
            // Find all validation directories
            File[] validationDirs = dataDir.listFiles(file -> 
                file.isDirectory() && file.getName().startsWith("validation-"));
            
            if (validationDirs == null || validationDirs.length == 0) {
                return;
            }
            
            // Sort by creation time (most recent first)
            Arrays.sort(validationDirs, Comparator.comparing(File::lastModified).reversed());
            
            // Keep only the most recent MAX_FILES_PER_TYPE validation directories
            for (int i = MAX_FILES_PER_TYPE; i < validationDirs.length; i++) {
                deleteDirectory(validationDirs[i]);
                logger.info("Deleted old validation directory: {}", validationDirs[i].getName());
            }
        } catch (Exception e) {
            logger.error("Error cleaning up validation files", e);
        }
    }
    
    /**
     * Cleans up large crawl.json files.
     */
    private static void cleanupLargeCrawlFiles(File dataDir) {
        try {
            // Find all project directories
            File[] projectDirs = dataDir.listFiles(File::isDirectory);
            
            if (projectDirs == null) {
                return;
            }
            
            for (File projectDir : projectDirs) {
                if (projectDir.getName().equals("ai-memory") || 
                    projectDir.getName().equals("conversations")) {
                    continue; // Skip special directories
                }
                
                // Check for large crawl.json files
                File crawlFile = new File(projectDir, "crawl.json");
                if (crawlFile.exists() && isLargeFile(crawlFile)) {
                    // Check if chunked files exist
                    boolean hasChunks = false;
                    for (int i = 0; i < 10; i++) { // Check for up to 10 chunks
                        if (new File(projectDir, "crawl_chunk" + i + ".json").exists()) {
                            hasChunks = true;
                            break;
                        }
                    }
                    
                    // If we have chunks, we can safely remove the large file
                    if (hasChunks) {
                        crawlFile.delete();
                        logger.info("Deleted large crawl.json file for project: {} (using chunks instead)", 
                            projectDir.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up large crawl files", e);
        }
    }
    
    /**
     * Checks if a file is larger than the maximum allowed size.
     */
    private static boolean isLargeFile(File file) {
        return file.length() > MAX_FILE_SIZE_MB * 1024 * 1024;
    }
    
    /**
     * Deletes a directory and all its contents.
     */
    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    /**
     * Gets a formatted timestamp for a file.
     */
    public static String getFileTimestamp(File file) {
        LocalDateTime date = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(file.lastModified()), 
            ZoneId.systemDefault());
        return date.toString();
    }
} 