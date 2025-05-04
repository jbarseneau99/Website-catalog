package com.spacedataarchive.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard response format for all API endpoints.
 * 
 * @param <T> The type of data contained in the response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private boolean success;
    private String message;
    private T data;
    private String error;
    
    /**
     * Creates a successful response with data.
     * 
     * @param data The data to include in the response
     * @param message A message describing the success
     * @param <T> The type of data
     * @return An ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
    
    /**
     * Creates a successful response with just data.
     * 
     * @param data The data to include in the response
     * @param <T> The type of data
     * @return An ApiResponse instance
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }
    
    /**
     * Creates an error response.
     * 
     * @param error The error message
     * @param <T> The type of data (will be null)
     * @return An ApiResponse instance
     */
    public static <T> ApiResponse<T> error(String error) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setError(error);
        response.setMessage("Operation failed");
        return response;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public void setData(T data) {
        this.data = data;
    }
    public void setError(String error) {
        this.error = error;
    }
} 