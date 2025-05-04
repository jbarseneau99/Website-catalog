package com.spacedataarchive.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to clean up corrupted memory files.
 * This can be run as a standalone tool to fix JSON parsing issues.
 */
public class MemoryFileCleanup {
    private static final Logger logger = LoggerFactory.getLogger(MemoryFileCleanup.class);
    private static final String MEMORY_DIR = "data/ai-memory";
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    /**
     * Main method to run the cleanup.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("Starting memory file cleanup");
        
        try {
            Path memoryDirPath = Paths.get(MEMORY_DIR);
            if (!Files.exists(memoryDirPath)) {
                logger.error("Memory directory does not exist: {}", MEMORY_DIR);
                return;
            }
            
            File memoryDir = memoryDirPath.toFile();
            File[] jsonFiles = memoryDir.listFiles((dir, name) -> name.endsWith(".json"));
            
            if (jsonFiles == null || jsonFiles.length == 0) {
                logger.info("No JSON files found in memory directory");
                return;
            }
            
            logger.info("Found {} JSON files to check", jsonFiles.length);
            
            int fixedCount = 0;
            for (File file : jsonFiles) {
                if (fixCorruptedFile(file)) {
                    fixedCount++;
                }
            }
            
            logger.info("Cleanup complete. Fixed {} corrupted files", fixedCount);
            
        } catch (Exception e) {
            logger.error("Error during memory file cleanup", e);
        }
    }
    
    /**
     * Attempts to fix a potentially corrupted memory file.
     * 
     * @param file The JSON file to fix
     * @return True if the file was fixed, false otherwise
     */
    private static boolean fixCorruptedFile(File file) {
        try {
            // Try to parse the file
            mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(ArrayList.class, Object.class));
            logger.info("File is valid: {}", file.getName());
            return false;
        } catch (Exception e) {
            logger.warn("Corrupted file detected: {}", file.getName());
            
            try {
                // Create a backup of the corrupted file
                Path originalPath = file.toPath();
                Path backupPath = Paths.get(file.getParent(), file.getName() + ".bak");
                Files.copy(originalPath, backupPath);
                logger.info("Created backup: {}", backupPath);
                
                // Create a new empty file
                List<Object> emptyList = new ArrayList<>();
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, emptyList);
                logger.info("Created new empty file: {}", file.getName());
                
                return true;
            } catch (IOException ex) {
                logger.error("Failed to fix corrupted file: {}", file.getName(), ex);
                return false;
            }
        }
    }
} 