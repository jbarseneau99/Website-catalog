package com.spacearchive.diagnostics;

import java.util.List;

/**
 * Represents deep analysis information from URL diagnostics
 */
public class DeepAnalysis {
    private String pageType;
    private DynamicLoading dynamicLoading;
    private List<PotentialIssue> potentialIssues;
    
    /**
     * Default constructor
     */
    public DeepAnalysis() {
    }
    
    /**
     * Constructor with initial values
     * 
     * @param pageType Type of page detected
     */
    public DeepAnalysis(String pageType) {
        this.pageType = pageType;
    }
    
    /**
     * Constructor with all values
     * 
     * @param pageType Type of page detected
     * @param dynamicLoading Dynamic loading information
     * @param potentialIssues List of potential issues
     */
    public DeepAnalysis(String pageType, DynamicLoading dynamicLoading, List<PotentialIssue> potentialIssues) {
        this.pageType = pageType;
        this.dynamicLoading = dynamicLoading;
        this.potentialIssues = potentialIssues;
    }
    
    // Getters and Setters
    
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