package com.spacedataarchive.service;

import com.spacedataarchive.model.CatalogProcessingResult;
import com.spacedataarchive.model.ProcessingJob;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service interface for the comprehensive catalog processing framework.
 * Defines the four main phases of processing: mapping, validation, extraction, and enhancement.
 */
public interface CatalogProcessingService {
    
    /**
     * Constants for processing steps
     */
    String STEP_MAPPING = "mapping";
    String STEP_VALIDATION = "validation";
    String STEP_EXTRACTION = "extraction";
    String STEP_ENHANCEMENT = "enhancement";
    
    /**
     * Creates a new processing job.
     *
     * @param name The name of the job
     * @param seedUrl The seed URL to start mapping from
     * @param maxDepth The maximum crawl depth
     * @param maxUrls The maximum number of URLs to process
     * @return The created processing job
     */
    ProcessingJob createJob(String name, String seedUrl, int maxDepth, int maxUrls);
    
    /**
     * Gets an existing processing job.
     *
     * @param jobId The job ID
     * @return The processing job
     */
    ProcessingJob getJob(String jobId);
    
    /**
     * Lists all processing jobs.
     *
     * @return A list of all processing jobs
     */
    List<ProcessingJob> listJobs();
    
    /**
     * Starts the mapping phase of processing.
     *
     * @param jobId The job ID
     * @param statusCallback Callback for status updates
     * @param resultsCallback Callback for result updates
     * @return A CompletableFuture that completes when mapping is done
     */
    CompletableFuture<CatalogProcessingResult> startMapping(
            String jobId, 
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback);
    
    /**
     * Starts the validation phase of processing.
     *
     * @param jobId The job ID
     * @param statusCallback Callback for status updates
     * @param resultsCallback Callback for result updates
     * @return A CompletableFuture that completes when validation is done
     */
    CompletableFuture<CatalogProcessingResult> startValidation(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback);
    
    /**
     * Starts the extraction phase of processing.
     *
     * @param jobId The job ID
     * @param statusCallback Callback for status updates
     * @param resultsCallback Callback for result updates
     * @return A CompletableFuture that completes when extraction is done
     */
    CompletableFuture<CatalogProcessingResult> startExtraction(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback);
    
    /**
     * Starts the enhancement phase of processing.
     *
     * @param jobId The job ID
     * @param statusCallback Callback for status updates
     * @param resultsCallback Callback for result updates
     * @return A CompletableFuture that completes when enhancement is done
     */
    CompletableFuture<CatalogProcessingResult> startEnhancement(
            String jobId,
            Consumer<String> statusCallback,
            Consumer<CatalogProcessingResult> resultsCallback);
    
    /**
     * Gets the status of a processing job.
     *
     * @param jobId The job ID
     * @return A map containing status information
     */
    Map<String, Object> getJobStatus(String jobId);
    
    /**
     * Stops a running processing job.
     *
     * @param jobId The job ID
     * @return True if the job was stopped successfully
     */
    boolean stopJob(String jobId);
} 