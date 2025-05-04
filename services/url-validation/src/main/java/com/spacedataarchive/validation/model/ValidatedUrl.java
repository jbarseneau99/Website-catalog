package com.spacedataarchive.validation.model;

import com.spacedataarchive.common.model.ValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB document model for storing validated URLs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "validated_urls")
public class ValidatedUrl {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String url;
    
    private boolean valid;
    private String error;
    private int statusCode;
    private long responseTimeMs;
    private String contentType;
    private long contentLengthBytes;
    private boolean redirect;
    private String redirectUrl;
    private ValidationStatus status;
    private LocalDateTime validatedAt;
    private LocalDateTime expiresAt;
    
    /**
     * Checks if this validation result has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
} 