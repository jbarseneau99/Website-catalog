package com.spacedataarchive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spacedataarchive.service.ai.AIMemoryService;
import com.spacedataarchive.service.ai.AIServiceManager;
import com.spacedataarchive.service.ai.AIMemoryService.AIFunctionMetadata;
import com.spacedataarchive.model.AIMessage;

import java.util.List;

/**
 * Utility class for testing the AI services.
 * Can be run as a standalone application to verify that all services are working properly.
 */
public class AIServiceTester {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceTester.class);
    
    /**
     * Main method for running the tester.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting AI Service Tester");
            
            // Initialize the AI Service Manager
            AIServiceManager manager = AIServiceManager.getInstance();
            
            // Get the memory service
            AIMemoryService memoryService = manager.getMemoryService();
            
            // List all registered functions
            List<AIFunctionMetadata> functions = memoryService.getAllFunctions();
            
            logger.info("Found {} AI functions registered:", functions.size());
            for (AIFunctionMetadata function : functions) {
                logger.info("  Function: {} ({})", function.getName(), function.getId());
                logger.info("    Type: {}", function.getType());
                logger.info("    Description: {}", function.getDescription());
                
                // Get messages for this function
                List<AIMessage> messages = memoryService.getMessages(function.getId());
                logger.info("    Message Count: {}", messages != null ? messages.size() : 0);
                
                logger.info("-----------------------");
            }
            
            logger.info("AI Service Tester completed successfully");
        } catch (Exception e) {
            logger.error("Error testing AI services", e);
        }
    }
} 