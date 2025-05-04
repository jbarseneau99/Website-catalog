package com.spacearchive.diagnostics;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a summary of URL diagnostics analysis results
 */
public class SummaryResult {
    private int total;
    private int success;
    private int redirects;
    private int errors;
    private int emptyContent;
    private int noAssets;
    private int withAssets;
    private Map<String, Integer> issueSummary = new HashMap<>();
    
    /**
     * Default constructor
     */
    public SummaryResult() {
    }
    
    /**
     * Constructor with initial values
     * 
     * @param total Total number of URLs analyzed
     * @param success Number of successfully analyzed URLs
     * @param redirects Number of redirects encountered
     * @param errors Number of errors encountered
     * @param emptyContent Number of URLs with empty content
     * @param noAssets Number of URLs with no assets
     * @param withAssets Number of URLs with assets
     */
    public SummaryResult(int total, int success, int redirects, int errors, int emptyContent, int noAssets, int withAssets) {
        this.total = total;
        this.success = success;
        this.redirects = redirects;
        this.errors = errors;
        this.emptyContent = emptyContent;
        this.noAssets = noAssets;
        this.withAssets = withAssets;
    }
    
    // Getters and Setters
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    public int getSuccess() {
        return success;
    }
    
    public void setSuccess(int success) {
        this.success = success;
    }
    
    public int getRedirects() {
        return redirects;
    }
    
    public void setRedirects(int redirects) {
        this.redirects = redirects;
    }
    
    public int getErrors() {
        return errors;
    }
    
    public void setErrors(int errors) {
        this.errors = errors;
    }
    
    public int getEmptyContent() {
        return emptyContent;
    }
    
    public void setEmptyContent(int emptyContent) {
        this.emptyContent = emptyContent;
    }
    
    public int getNoAssets() {
        return noAssets;
    }
    
    public void setNoAssets(int noAssets) {
        this.noAssets = noAssets;
    }
    
    public int getWithAssets() {
        return withAssets;
    }
    
    public void setWithAssets(int withAssets) {
        this.withAssets = withAssets;
    }
    
    public Map<String, Integer> getIssueSummary() {
        return issueSummary;
    }
    
    public void setIssueSummary(Map<String, Integer> issueSummary) {
        this.issueSummary = issueSummary;
    }
    
    /**
     * Calculate the success rate as a percentage
     * 
     * @return Success rate percentage (0-100)
     */
    public double getSuccessRate() {
        return total > 0 ? (double) success / total * 100 : 0;
    }
    
    /**
     * Calculate the asset detection rate
     * 
     * @return Asset detection rate percentage (0-100)
     */
    public double getAssetDetectionRate() {
        return success > 0 ? (double) withAssets / success * 100 : 0;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary Result:\n");
        sb.append("- Total URLs: ").append(total).append("\n");
        sb.append("- Successful: ").append(success).append(" (").append(String.format("%.1f%%", getSuccessRate())).append(")\n");
        sb.append("- Redirects: ").append(redirects).append("\n");
        sb.append("- Errors: ").append(errors).append("\n");
        sb.append("- Empty Content: ").append(emptyContent).append("\n");
        sb.append("- URLs with assets: ").append(withAssets).append("\n");
        sb.append("- URLs without assets: ").append(noAssets).append("\n");
        
        if (!issueSummary.isEmpty()) {
            sb.append("Issue Summary:\n");
            for (Map.Entry<String, Integer> entry : issueSummary.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return sb.toString();
    }
} 