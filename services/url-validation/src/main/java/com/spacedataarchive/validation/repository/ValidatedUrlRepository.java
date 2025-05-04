package com.spacedataarchive.validation.repository;

import com.spacedataarchive.validation.model.ValidatedUrl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for ValidatedUrl documents.
 */
@Repository
public interface ValidatedUrlRepository extends MongoRepository<ValidatedUrl, String> {
    
    /**
     * Find a validated URL by its URL string.
     *
     * @param url The URL to find
     * @return Optional containing the validation result if found
     */
    Optional<ValidatedUrl> findByUrl(String url);
    
    /**
     * Delete all expired validation results.
     *
     * @param dateTime The date/time threshold for expiration
     * @return The number of documents deleted
     */
    long deleteByExpiresAtBefore(LocalDateTime dateTime);
} 