package com.spacedataarchive.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a Site Map Project, the central entity that connects
 * all steps in the crawling and processing pipeline.
 */
public class SiteMapProject {
    private final String id;
    private String name;
    private String seedUrl;
    private int crawlDepth;
    private int maxPages;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private String status;
    
    /**
     * Creates a new Site Map Project with a generated UUID.
     */
    public SiteMapProject() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastModified = this.createdAt;
        this.status = "Created";
    }
    
    /**
     * Creates a new Site Map Project with the specified parameters.
     * 
     * @param name The project name
     * @param seedUrl The seed URL to start crawling from
     * @param crawlDepth The maximum crawl depth
     * @param maxPages The maximum number of pages to crawl
     */
    public SiteMapProject(String name, String seedUrl, int crawlDepth, int maxPages) {
        this();
        this.name = name;
        this.seedUrl = seedUrl;
        this.crawlDepth = crawlDepth;
        this.maxPages = maxPages;
    }

    // Getters and setters
    
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.lastModified = LocalDateTime.now();
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
        this.lastModified = LocalDateTime.now();
    }

    public int getCrawlDepth() {
        return crawlDepth;
    }

    public void setCrawlDepth(int crawlDepth) {
        this.crawlDepth = crawlDepth;
        this.lastModified = LocalDateTime.now();
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
        this.lastModified = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastModified = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return name + " (" + seedUrl + ")";
    }
} 