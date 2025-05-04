package com.spacearchive.diagnostics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a URL diagnostic result
 */
public class DiagnosticResult {
    private String url;
    private int statusCode;
    private boolean redirected;
    private String redirectUrl;
    private String contentType;
    private long contentLength;
    private AssetCounts assetCounts;
    private DeepAnalysis deepAnalysis;
    
    /**
     * Default constructor for Jackson deserialization
     */
    public DiagnosticResult() {
    }
    
    /**
     * Constructor with basic fields
     */
    public DiagnosticResult(String url, int statusCode) {
        this.url = url;
        this.statusCode = statusCode;
    }
    
    // Getters and setters
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public boolean isRedirected() {
        return redirected;
    }
    
    public void setRedirected(boolean redirected) {
        this.redirected = redirected;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }
    
    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public long getContentLength() {
        return contentLength;
    }
    
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
    
    public AssetCounts getAssetCounts() {
        return assetCounts;
    }
    
    public void setAssetCounts(AssetCounts assetCounts) {
        this.assetCounts = assetCounts;
    }
    
    public DeepAnalysis getDeepAnalysis() {
        return deepAnalysis;
    }
    
    public void setDeepAnalysis(DeepAnalysis deepAnalysis) {
        this.deepAnalysis = deepAnalysis;
    }
    
    /**
     * Asset counts information
     */
    public static class AssetCounts {
        private int documentCount;
        private int imageCount;
        private int totalAssets;
        private boolean archiveSection;
        private String archiveType;
        private double archiveScore;
        
        public int getDocumentCount() {
            return documentCount;
        }
        
        public void setDocumentCount(int documentCount) {
            this.documentCount = documentCount;
        }
        
        public int getImageCount() {
            return imageCount;
        }
        
        public void setImageCount(int imageCount) {
            this.imageCount = imageCount;
        }
        
        public int getTotalAssets() {
            return totalAssets;
        }
        
        public void setTotalAssets(int totalAssets) {
            this.totalAssets = totalAssets;
        }
        
        public boolean isArchiveSection() {
            return archiveSection;
        }
        
        public void setArchiveSection(boolean archiveSection) {
            this.archiveSection = archiveSection;
        }
        
        public String getArchiveType() {
            return archiveType;
        }
        
        public void setArchiveType(String archiveType) {
            this.archiveType = archiveType;
        }
        
        public double getArchiveScore() {
            return archiveScore;
        }
        
        public void setArchiveScore(double archiveScore) {
            this.archiveScore = archiveScore;
        }
    }
    
    /**
     * Deep analysis information
     */
    public static class DeepAnalysis {
        private String pageType;
        private DynamicLoading dynamicLoading;
        private List<PotentialIssue> potentialIssues = new ArrayList<>();
        
        public String getPageType() {
            return pageType;
        }
        
        public void setPageType(String pageType) {
            this.pageType = pageType;
        }
        
        public DynamicLoading getDynamicLoading() {
            return dynamicLoading;
        }
        
        public void setDynamicLoading(DynamicLoading dynamicLoading) {
            this.dynamicLoading = dynamicLoading;
        }
        
        public List<PotentialIssue> getPotentialIssues() {
            return potentialIssues;
        }
        
        public void setPotentialIssues(List<PotentialIssue> potentialIssues) {
            this.potentialIssues = potentialIssues;
        }
    }
    
    /**
     * Dynamic loading information
     */
    public static class DynamicLoading {
        private boolean hasAjaxLoaders;
        private boolean hasLazyLoading;
        private int jsLoaders;
        private int jsIncludes;
        
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
    
    /**
     * Potential issue information
     */
    public static class PotentialIssue {
        private String type;
        private String description;
        private List<String> iframeUrls = new ArrayList<>();
        private Map<String, Integer> containers = new HashMap<>();
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public List<String> getIframeUrls() {
            return iframeUrls;
        }
        
        public void setIframeUrls(List<String> iframeUrls) {
            this.iframeUrls = iframeUrls;
        }
        
        public Map<String, Integer> getContainers() {
            return containers;
        }
        
        public void setContainers(Map<String, Integer> containers) {
            this.containers = containers;
        }
    }
} 