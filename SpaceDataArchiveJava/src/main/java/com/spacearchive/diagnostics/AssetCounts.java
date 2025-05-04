package com.spacearchive.diagnostics;

/**
 * Represents asset count information from URL diagnostics
 */
public class AssetCounts {
    private int documentCount;
    private int imageCount;
    private int videoCount;
    private int datasetCount;
    private int totalAssets;
    private boolean isArchiveSection;
    private String archiveType;
    private double archiveScore;
    
    /**
     * Default constructor
     */
    public AssetCounts() {
    }
    
    /**
     * Constructor with initial values
     * 
     * @param documentCount Number of document assets
     * @param imageCount Number of image assets
     * @param videoCount Number of video assets
     * @param datasetCount Number of dataset assets
     * @param totalAssets Total number of assets
     */
    public AssetCounts(int documentCount, int imageCount, int videoCount, int datasetCount, int totalAssets) {
        this.documentCount = documentCount;
        this.imageCount = imageCount;
        this.videoCount = videoCount;
        this.datasetCount = datasetCount;
        this.totalAssets = totalAssets;
    }
    
    /**
     * Constructor with all values
     * 
     * @param documentCount Number of document assets
     * @param imageCount Number of image assets
     * @param videoCount Number of video assets
     * @param datasetCount Number of dataset assets
     * @param totalAssets Total number of assets
     * @param isArchiveSection Whether the page is an archive section
     * @param archiveType Type of archive section
     * @param archiveScore Score indicating confidence in archive classification
     */
    public AssetCounts(int documentCount, int imageCount, int videoCount, int datasetCount, int totalAssets,
                       boolean isArchiveSection, String archiveType, double archiveScore) {
        this.documentCount = documentCount;
        this.imageCount = imageCount;
        this.videoCount = videoCount;
        this.datasetCount = datasetCount;
        this.totalAssets = totalAssets;
        this.isArchiveSection = isArchiveSection;
        this.archiveType = archiveType;
        this.archiveScore = archiveScore;
    }
    
    // Getters and Setters
    
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
    
    public int getVideoCount() {
        return videoCount;
    }
    
    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }
    
    public int getDatasetCount() {
        return datasetCount;
    }
    
    public void setDatasetCount(int datasetCount) {
        this.datasetCount = datasetCount;
    }
    
    public int getTotalAssets() {
        return totalAssets;
    }
    
    public void setTotalAssets(int totalAssets) {
        this.totalAssets = totalAssets;
    }
    
    public boolean isArchiveSection() {
        return isArchiveSection;
    }
    
    public void setArchiveSection(boolean archiveSection) {
        isArchiveSection = archiveSection;
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