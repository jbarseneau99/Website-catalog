package com.spacedataarchive.service;

import com.spacedataarchive.service.ai.AIMemoryService;

/**
 * Factory class to create and manage service instances.
 */
public class ServiceFactory {
    private static ServiceFactory instance;
    
    // Services
    private final LogService logService;
    private final StorageService storageService;
    private final SiteMapService siteMapService;
    private final URLValidationService urlValidationService;
    private final AIMemoryService aiMemoryService;
    private final CatalogProcessingService catalogProcessingService;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private ServiceFactory() {
        // Initialize services in the correct order
        
        // Core services
        logService = new LogService();
        storageService = StorageService.getInstance();
        
        // AI services
        aiMemoryService = new AIMemoryService();
        
        // Domain services
        siteMapService = SiteMapService.getInstance();
        urlValidationService = new URLValidationServiceImpl(siteMapService, aiMemoryService);
        catalogProcessingService = new CatalogProcessingServiceImpl(siteMapService, urlValidationService);
    }
    
    /**
     * Gets the singleton instance of the ServiceFactory.
     * 
     * @return The ServiceFactory instance
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    /**
     * Gets the LogService.
     * 
     * @return The LogService
     */
    public LogService getLogService() {
        return logService;
    }
    
    /**
     * Gets the StorageService.
     * 
     * @return The StorageService
     */
    public StorageService getStorageService() {
        return storageService;
    }
    
    /**
     * Gets the SiteMapService.
     * 
     * @return The SiteMapService
     */
    public SiteMapService getSiteMapService() {
        return siteMapService;
    }
    
    /**
     * Gets the URLValidationService.
     * 
     * @return The URLValidationService
     */
    public URLValidationService getUrlValidationService() {
        return urlValidationService;
    }
    
    /**
     * Gets the AIMemoryService.
     * 
     * @return The AIMemoryService
     */
    public AIMemoryService getAiMemoryService() {
        return aiMemoryService;
    }
    
    /**
     * Gets the CatalogProcessingService.
     * 
     * @return The CatalogProcessingService
     */
    public CatalogProcessingService getCatalogProcessingService() {
        return catalogProcessingService;
    }
    
    /**
     * Shuts down all services gracefully in the reverse order of initialization.
     */
    public void shutdown() {
        // Add logging
        System.out.println("ServiceFactory: Starting service shutdown sequence");
        
        // First shutdown application-level services
        try {
            if (catalogProcessingService != null) {
                ((CatalogProcessingServiceImpl) catalogProcessingService).shutdown();
                System.out.println("ServiceFactory: CatalogProcessingService shut down successfully");
            }
        } catch (Exception e) {
            System.err.println("ServiceFactory: Error shutting down CatalogProcessingService: " + e.getMessage());
        }
        
        // Then shutdown validation services
        try {
            if (urlValidationService != null) {
                ((URLValidationServiceImpl) urlValidationService).shutdown();
                System.out.println("ServiceFactory: URLValidationService shut down successfully");
            }
        } catch (Exception e) {
            System.err.println("ServiceFactory: Error shutting down URLValidationService: " + e.getMessage());
        }
        
        // Then shutdown site map service
        try {
            if (siteMapService != null) {
                // Note: SiteMapService doesn't have a shutdown method yet
                // We would call: siteMapService.shutdown() if it existed
                System.out.println("ServiceFactory: SiteMapService resources released");
            }
        } catch (Exception e) {
            System.err.println("ServiceFactory: Error releasing SiteMapService resources: " + e.getMessage());
        }
        
        // Shutdown AI services
        try {
            if (aiMemoryService != null) {
                // If AIMemoryService has a shutdown method, call it
                // aiMemoryService.shutdown();
                System.out.println("ServiceFactory: AIMemoryService resources released");
            }
        } catch (Exception e) {
            System.err.println("ServiceFactory: Error releasing AIMemoryService resources: " + e.getMessage());
        }
        
        // Finally, shutdown core services
        try {
            if (storageService != null) {
                // Note: StorageService doesn't have a flush method yet
                // We would call: storageService.flush() if it existed
                System.out.println("ServiceFactory: StorageService resources released");
            }
        } catch (Exception e) {
            System.err.println("ServiceFactory: Error releasing StorageService resources: " + e.getMessage());
        }
        
        System.out.println("ServiceFactory: All services shut down");
    }
} 