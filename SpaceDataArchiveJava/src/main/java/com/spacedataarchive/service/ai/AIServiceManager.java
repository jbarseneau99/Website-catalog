package com.spacedataarchive.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class that initializes and provides access to all AI services.
 * This centralized approach ensures all services are properly registered.
 */
public class AIServiceManager {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceManager.class);

    private static AIServiceManager instance;
    
    private final AIMemoryService memoryService;
    private final AnthropicAIService aiService;
    private final ProxyBalancerAIService proxyBalancerService;
    
    /**
     * Private constructor for singleton pattern.
     */
    private AIServiceManager() {
        logger.info("Initializing AI Service Manager");
        
        // Initialize base services
        this.memoryService = new AIMemoryService();
        this.aiService = new AnthropicAIService();
        
        // Initialize specialized AI services
        this.proxyBalancerService = new ProxyBalancerAIService(aiService, memoryService);
        
        logger.info("AI Service Manager initialization complete");
    }
    
    /**
     * Get the singleton instance.
     * 
     * @return The AIServiceManager instance
     */
    public static synchronized AIServiceManager getInstance() {
        if (instance == null) {
            instance = new AIServiceManager();
        }
        return instance;
    }
    
    /**
     * Get the memory service.
     * 
     * @return The AIMemoryService
     */
    public AIMemoryService getMemoryService() {
        return memoryService;
    }
    
    /**
     * Get the Anthropic AI service.
     * 
     * @return The AnthropicAIService
     */
    public AnthropicAIService getAiService() {
        return aiService;
    }
    
    /**
     * Get the discovery AI service.
     * Temporary stub until proper implementation is restored.
     * 
     * @return null - service currently unavailable
     */
    public DiscoveryAIService getDiscoveryService() {
        logger.warn("DiscoveryAIService was requested but is not currently available");
        return null;
    }
    
    /**
     * Get the proxy balancer AI service.
     * 
     * @return The ProxyBalancerAIService
     */
    public ProxyBalancerAIService getProxyBalancerService() {
        return proxyBalancerService;
    }
} 