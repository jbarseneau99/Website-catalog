package com.spacedataarchive.common.model;

/**
 * Represents the status of a URL validation operation.
 */
public enum ValidationStatus {
    /**
     * The validation is pending and has not started yet.
     */
    PENDING,

    /**
     * The validation is currently in progress.
     */
    IN_PROGRESS,
    
    /**
     * The validation completed successfully.
     */
    SUCCESS,
    
    /**
     * The validation failed due to an error.
     */
    ERROR,
    
    /**
     * The validation was skipped (e.g., URL already validated).
     */
    SKIPPED,
    
    /**
     * The validation timed out.
     */
    TIMEOUT,
    
    /**
     * The validation was cancelled.
     */
    CANCELLED
}