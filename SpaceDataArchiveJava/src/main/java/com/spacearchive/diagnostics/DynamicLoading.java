package com.spacearchive.diagnostics;

/**
 * Represents dynamic content loading information from URL diagnostics
 */
public class DynamicLoading {
    private boolean hasAjaxLoaders;
    private boolean hasLazyLoading;
    private int jsLoaders;
    private int jsIncludes;
    
    /**
     * Default constructor
     */
    public DynamicLoading() {
    }
    
    /**
     * Constructor with initial values
     * 
     * @param hasAjaxLoaders Whether AJAX loaders were detected
     * @param hasLazyLoading Whether lazy loading was detected
     * @param jsLoaders Number of JavaScript loaders
     * @param jsIncludes Number of JavaScript includes
     */
    public DynamicLoading(boolean hasAjaxLoaders, boolean hasLazyLoading, int jsLoaders, int jsIncludes) {
        this.hasAjaxLoaders = hasAjaxLoaders;
        this.hasLazyLoading = hasLazyLoading;
        this.jsLoaders = jsLoaders;
        this.jsIncludes = jsIncludes;
    }
    
    // Getters and Setters
    
    public boolean isHasAjaxLoaders() {
        return hasAjaxLoaders;
    }
    
    public void setHasAjaxLoaders(boolean hasAjaxLoaders) {
        this.hasAjaxLoaders = hasAjaxLoaders;
    }
    
    public boolean isHasLazyLoading() {
        return hasLazyLoading;
    }
    
    public void setHasLazyLoading(boolean hasLazyLoading) {
        this.hasLazyLoading = hasLazyLoading;
    }
    
    public int getJsLoaders() {
        return jsLoaders;
    }
    
    public void setJsLoaders(int jsLoaders) {
        this.jsLoaders = jsLoaders;
    }
    
    public int getJsIncludes() {
        return jsIncludes;
    }
    
    public void setJsIncludes(int jsIncludes) {
        this.jsIncludes = jsIncludes;
    }
} 