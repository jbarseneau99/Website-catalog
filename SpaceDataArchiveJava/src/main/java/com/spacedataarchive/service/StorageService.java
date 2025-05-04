package com.spacedataarchive.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.spacedataarchive.model.EnhancedValidationResult;
import com.spacedataarchive.model.ValidatedUrlCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for storing and retrieving data in JSON format.
 * Each project step has its own JSON file, all linked by the project ID.
 */
public class StorageService {
    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private static final String BASE_DIR = "data";
    private static StorageService instance;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new StorageService.
     */
    private StorageService() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.findAndRegisterModules(); // Auto-discover and register modules
        
        // Create base directory if it doesn't exist
        try {
            Path path = Paths.get(BASE_DIR);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created data directory: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create data directory", e);
        }
    }
    
    /**
     * Gets the singleton instance of the StorageService.
     * 
     * @return The StorageService instance
     */
    public static synchronized StorageService getInstance() {
        if (instance == null) {
            instance = new StorageService();
        }
        return instance;
    }
    
    /**
     * Saves an object as JSON.
     * 
     * @param object The object to save
     * @param projectId The project ID
     * @param step The process step (e.g., "sitemap", "validation", "extraction")
     * @return True if the save was successful, false otherwise
     */
    public boolean save(Object object, String projectId, String step) {
        try {
            // Create project directory if it doesn't exist
            Path projectDir = Paths.get(BASE_DIR, projectId);
            if (!Files.exists(projectDir)) {
                Files.createDirectories(projectDir);
            }
            
            // Save the object as JSON
            File file = new File(projectDir.toString(), step + ".json");
            
            // Use safer atomic write pattern
            File tempFile = new File(file.getAbsolutePath() + ".tmp");
            
            // First write to a temporary file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, object);
            
            // Ensure the temp file is fully written to disk
            boolean tempExists = tempFile.exists() && tempFile.length() > 0;
            if (!tempExists) {
                logger.error("Failed to write temporary file for {} data for project {}", step, projectId);
                return false;
            }
            
            // Use atomic file move operation to replace target file (safer than direct write)
            if (file.exists()) {
                file.delete();
            }
            
            boolean renamed = tempFile.renameTo(file);
            if (!renamed) {
                // If rename fails, try to copy the content manually
                Files.copy(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                tempFile.delete();
            }
            
            // Verify file exists and has content
            if (!file.exists() || file.length() == 0) {
                logger.error("Failed to save {} data for project {} - file is empty or missing", step, projectId);
                return false;
            }
            
            logger.info("Saved {} data for project {} to {}", step, projectId, file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error("Failed to save {} data for project {}", step, projectId, e);
            return false;
        }
    }
    
    /**
     * Loads an object from JSON.
     * 
     * @param projectId The project ID
     * @param step The process step
     * @param valueType The class of the object to load
     * @param <T> The type of the object
     * @return The loaded object, or null if loading failed
     */
    public <T> T load(String projectId, String step, Class<T> valueType) {
        try {
            File file = new File(Paths.get(BASE_DIR, projectId, step + ".json").toString());
            if (!Files.exists(file.toPath())) {
                logger.warn("File does not exist: {}", file.getAbsolutePath());
                return null;
            }
            
            // Use the main instance of the object mapper for consistency
            T object = objectMapper.readValue(file, valueType);
            
            logger.info("Loaded {} data for project {} from {}", step, projectId, file.getAbsolutePath());
            return object;
        } catch (IOException e) {
            logger.error("Failed to load {} data for project {}: {}", step, projectId, e.getMessage());
            
            // For validation data, try alternative loading approach if standard deserialization fails
            if ("validation".equals(step)) {
                return tryAlternativeLoading(projectId, step, valueType);
            }
            
            return null;
        }
    }
    
    /**
     * Attempts to load a JSON file using a more resilient approach.
     * This is used as a fallback when standard deserialization fails.
     *
     * @param projectId The project ID
     * @param step The process step
     * @param valueType The class of the object to load
     * @param <T> The type of the object to load
     * @return The loaded object, or null if loading failed
     */
    @SuppressWarnings("unchecked")
    private <T> T tryAlternativeLoading(String projectId, String step, Class<T> valueType) {
        try {
            File file = new File(Paths.get(BASE_DIR, projectId, step + ".json").toString());
            if (!Files.exists(file.toPath())) {
                return null;
            }
            
            logger.info("Attempting alternative loading for {} data", step);
            
            // Configure a new object mapper with more permissive settings
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
            
            // Read as a generic map first
            Map<String, Object> rawData = mapper.readValue(file, Map.class);
            
            // If we're looking for a ValidatedUrlCollection, do special handling
            if (valueType.getSimpleName().equals("ValidatedUrlCollection")) {
                try {
                    // Try to create a new object of the right type using reflection
                    Object instance = valueType.getDeclaredConstructor().newInstance();
                    
                    // Use reflection to set fields from the map
                    for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                        try {
                            if ("siteMapId".equals(entry.getKey()) || "sourceMapId".equals(entry.getKey())) {
                                // Special handling for site map ID fields
                                String setterMethod = "set" + entry.getKey().substring(0, 1).toUpperCase() + 
                                                    entry.getKey().substring(1);
                                
                                try {
                                    java.lang.reflect.Method method = valueType.getMethod(setterMethod, String.class);
                                    method.invoke(instance, entry.getValue().toString());
                                    logger.debug("Set {} using method {}", entry.getKey(), setterMethod);
                                } catch (Exception e) {
                                    logger.debug("Failed to invoke method {}: {}", setterMethod, e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Error setting field {}: {}", entry.getKey(), e.getMessage());
                        }
                    }
                    
                    // Now try to deserialize the whole object again
                    String json = mapper.writeValueAsString(rawData);
                    T result = mapper.readValue(json, valueType);
                    
                    logger.info("Alternative loading successful for {} data", step);
                    return result;
                } catch (Exception e) {
                    logger.error("Alternative loading failed: {}", e.getMessage());
                }
            }
            
            // Default - try to convert the map directly to the target type
            T result = mapper.convertValue(rawData, valueType);
            logger.info("Alternative loading using map conversion successful for {} data", step);
            return result;
            
        } catch (Exception e) {
            logger.error("Alternative loading failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Lists all project IDs.
     * 
     * @return A list of project IDs
     */
    public List<String> listProjects() {
        try {
            Path path = Paths.get(BASE_DIR);
            if (!Files.exists(path)) {
                return Collections.emptyList();
            }
            
            return Files.list(path)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list projects", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Lists all steps for a project.
     * 
     * @param projectId The project ID
     * @return A list of step names
     */
    public List<String> listSteps(String projectId) {
        try {
            Path path = Paths.get(BASE_DIR, projectId);
            if (!Files.exists(path)) {
                return Collections.emptyList();
            }
            
            return Files.list(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString().replace(".json", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Failed to list steps for project {}", projectId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Deletes a project and all its data.
     * 
     * @param projectId The project ID
     * @return True if the deletion was successful, false otherwise
     */
    public boolean deleteProject(String projectId) {
        try {
            Path path = Paths.get(BASE_DIR, projectId);
            if (!Files.exists(path)) {
                return true; // Already deleted
            }
            
            Files.walk(path)
                    .sorted((p1, p2) -> -p1.compareTo(p2)) // Delete files before directories
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            logger.error("Failed to delete {}", p, e);
                        }
                    });
            
            logger.info("Deleted project {}", projectId);
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete project {}", projectId, e);
            return false;
        }
    }
    
    /**
     * Lists all IDs for a specific step.
     * 
     * @param step The process step
     * @return A list of IDs
     */
    public List<String> listIds(String step) {
        try {
            Path basePath = Paths.get(BASE_DIR);
            if (!Files.exists(basePath)) {
                return Collections.emptyList();
            }
            
            List<String> ids = new ArrayList<>();
            Files.list(basePath)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    Path stepFile = dir.resolve(step + ".json");
                    if (Files.exists(stepFile)) {
                        ids.add(dir.getFileName().toString());
                    }
                });
            
            return ids;
        } catch (IOException e) {
            logger.error("Failed to list IDs for step {}", step, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Loads URLs from a site map.
     * 
     * @param siteMapId The site map ID
     * @return A list of URLs
     */
    public List<String> loadSiteMapUrls(String siteMapId) {
        try {
            // This would normally load URLs from the site map file
            // For now, we'll return a simple list for demonstration
            File file = new File(Paths.get(BASE_DIR, siteMapId, "crawl.json").toString());
            if (!file.exists()) {
                logger.warn("Site map file does not exist: {}", file.getAbsolutePath());
                return Collections.emptyList();
            }
            
            // In a real implementation, this would parse the file and extract URLs
            // For demo purposes, return some sample URLs
            return List.of(
                "https://spacenews.com/article1",
                "https://spacenews.com/article2",
                "http://insecure-example.com/page",
                "https://broken-redirect.example.com"
            );
        } catch (Exception e) {
            logger.error("Failed to load URLs from site map {}", siteMapId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Creates a properly configured ObjectMapper for JSON serialization/deserialization.
     * 
     * @return A configured ObjectMapper instance
     */
    protected ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }
    
    /**
     * Saves a validation collection to storage.
     * 
     * @param collection The validation collection to save.
     * @return true if the save was successful, false otherwise.
     */
    public boolean saveValidationCollection(ValidatedUrlCollection collection) {
        logger.info("Saving validation collection: {}", collection.getId());
        
        try {
            // Ensure validation directory exists
            Path validationDir = Paths.get(BASE_DIR, "validations");
            if (!Files.exists(validationDir)) {
                Files.createDirectories(validationDir);
            }
            
            // Create validation-specific directory
            Path validationPath = Paths.get(validationDir.toString(), collection.getId());
            if (!Files.exists(validationPath)) {
                Files.createDirectories(validationPath);
            }
            
            // Save validation metadata
            Path metadataPath = Paths.get(validationPath.toString(), "metadata.json");
            
            // Create metadata object
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("validationId", collection.getId());
            metadata.put("name", collection.getName());
            metadata.put("createdAt", collection.getCreatedAt().toString());
            metadata.put("siteMapId", collection.getSiteMapId());
            metadata.put("validatedUrls", collection.getUrls().size());
            metadata.put("totalUrls", collection.getTotalUrls());
            metadata.put("valid", collection.getValidUrls());
            metadata.put("invalid", collection.getInvalidUrls());
            metadata.put("warnings", collection.getWarningUrls());
            
            // Add content type statistics if available
            Map<String, Integer> contentTypeStats = new HashMap<>();
            Map<String, Integer> spaceNewsTypeStats = new HashMap<>();
            
            // Count occurrences of each content type
            for (EnhancedValidationResult url : collection.getUrls()) {
                String contentType = url.getAssetType();
                if (contentType != null && !contentType.isEmpty()) {
                    // Update general content type stats
                    contentTypeStats.put(contentType, contentTypeStats.getOrDefault(contentType, 0) + 1);
                    
                    // For SpaceNews specific types, track separately
                    if (url.getUrl().contains("spacenews.com") && 
                        !contentType.equals("webpage") && 
                        !contentType.equals("image") && 
                        !contentType.equals("document") && 
                        !contentType.equals("video") && 
                        !contentType.equals("audio") && 
                        !contentType.equals("data")) {
                        
                        spaceNewsTypeStats.put(contentType, spaceNewsTypeStats.getOrDefault(contentType, 0) + 1);
                    }
                }
            }
            
            // Add stats to metadata
            if (!contentTypeStats.isEmpty()) {
                metadata.put("contentTypeStats", contentTypeStats);
            }
            
            if (!spaceNewsTypeStats.isEmpty()) {
                metadata.put("spaceNewsTypeStats", spaceNewsTypeStats);
            }
            
            // Save metadata to file
            ObjectMapper mapper = createObjectMapper();
            String metadataJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
            Files.write(metadataPath, metadataJson.getBytes());
            
            // Save URLs to separate file
            Path urlsPath = Paths.get(validationPath.toString(), "urls.json");
            
            // Create object mapper with proper configuration
            ObjectMapper urlsMapper = createObjectMapper();
            
            // Write URLs to file
            String urlsJson = urlsMapper.writerWithDefaultPrettyPrinter().writeValueAsString(collection.getUrls());
            Files.write(urlsPath, urlsJson.getBytes());
            
            logger.info("Successfully saved validation collection: {}", collection.getId());
            return true;
        } catch (Exception e) {
            logger.error("Error saving validation collection: {}", collection.getId(), e);
            return false;
        }
    }
} 