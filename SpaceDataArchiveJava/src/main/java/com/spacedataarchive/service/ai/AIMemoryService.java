package com.spacedataarchive.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spacedataarchive.model.AIMessage;

/**
 * Service that manages memory segments for different AI functions.
 * Each AI function (tab, popup, function) has its own memory context.
 */
public class AIMemoryService {
    private static final Logger logger = LoggerFactory.getLogger(AIMemoryService.class);
    private static final String MEMORY_DIR = "data/ai-memory";
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    // Map of function ID to its memory segment (list of messages)
    private final Map<String, List<AIMessage>> memorySegments = new ConcurrentHashMap<>();
    
    // Map of function ID to its metadata
    private final Map<String, AIFunctionMetadata> functionRegistry = new ConcurrentHashMap<>();
    
    /**
     * Constructor that initializes the memory service.
     */
    public AIMemoryService() {
        initializeDirectories();
        loadRegistry();
    }
    
    /**
     * Creates the necessary directories for storing memory.
     */
    private void initializeDirectories() {
        try {
            Path memoryPath = Paths.get(MEMORY_DIR);
            if (!Files.exists(memoryPath)) {
                Files.createDirectories(memoryPath);
                logger.info("Created AI memory directory: {}", MEMORY_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create AI memory directory", e);
        }
    }
    
    /**
     * Loads the function registry from disk.
     */
    private void loadRegistry() {
        try {
            Path registryPath = Paths.get(MEMORY_DIR, "registry.json");
            if (Files.exists(registryPath)) {
                Map<String, AIFunctionMetadata> loadedRegistry = mapper.readValue(
                    registryPath.toFile(), 
                    mapper.getTypeFactory().constructMapType(
                        HashMap.class, String.class, AIFunctionMetadata.class)
                );
                functionRegistry.putAll(loadedRegistry);
                
                // Load memory for each function
                for (String functionId : functionRegistry.keySet()) {
                    loadMemorySegment(functionId);
                }
                
                logger.info("Loaded AI registry with {} functions", functionRegistry.size());
            }
        } catch (IOException e) {
            logger.error("Failed to load AI registry", e);
        }
    }
    
    /**
     * Saves the function registry to disk.
     */
    private void saveRegistry() {
        try {
            Path registryPath = Paths.get(MEMORY_DIR, "registry.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(registryPath.toFile(), functionRegistry);
            logger.info("Saved AI registry with {} functions", functionRegistry.size());
        } catch (IOException e) {
            logger.error("Failed to save AI registry", e);
        }
    }
    
    /**
     * Loads a memory segment for a function.
     * 
     * @param functionId The function ID
     */
    private void loadMemorySegment(String functionId) {
        Path memoryPath = Paths.get(MEMORY_DIR, functionId + ".json");
        List<AIMessage> messages = new ArrayList<>();
        
        try {
            if (Files.exists(memoryPath)) {
                try {
                    messages = mapper.readValue(
                        memoryPath.toFile(),
                        mapper.getTypeFactory().constructCollectionType(ArrayList.class, AIMessage.class)
                    );
                    logger.info("Loaded memory segment for function {} with {} messages", 
                        functionId, messages.size());
                } catch (Exception e) {
                    logger.warn("Failed to parse memory file for function {}. Will attempt to fix.", functionId, e);
                    
                    // Try to recover by converting the corrupted file to a compatible format
                    try {
                        // Rename the corrupted file to create a backup
                        Path backupPath = Paths.get(MEMORY_DIR, functionId + ".json.bak");
                        Files.copy(memoryPath, backupPath);
                        logger.info("Created backup of corrupted memory file: {}", backupPath);
                        
                        // Create a new empty memory segment
                        // Save the empty segment to create a valid file
                        mapper.writerWithDefaultPrettyPrinter().writeValue(memoryPath.toFile(), messages);
                        logger.info("Created new empty memory segment for function {}", functionId);
                    } catch (Exception ex) {
                        logger.error("Failed to recover corrupted memory file for function {}", functionId, ex);
                    }
                }
            } else {
                logger.info("No memory file exists for function {}, creating empty segment", functionId);
            }
        } catch (Exception e) {
            logger.error("Failed to process memory segment for function {}", functionId, e);
        }
        
        // Always store the messages, whether loaded or empty
        memorySegments.put(functionId, messages);
    }
    
    /**
     * Saves a memory segment for a function.
     * 
     * @param functionId The function ID
     */
    private void saveMemorySegment(String functionId) {
        try {
            List<AIMessage> messages = memorySegments.get(functionId);
            if (messages != null) {
                Path memoryPath = Paths.get(MEMORY_DIR, functionId + ".json");
                mapper.writerWithDefaultPrettyPrinter().writeValue(memoryPath.toFile(), messages);
                logger.info("Saved memory segment for function {} with {} messages", 
                    functionId, messages.size());
            }
        } catch (IOException e) {
            logger.error("Failed to save memory segment for function {}", functionId, e);
        }
    }
    
    /**
     * Registers a new AI function.
     * 
     * @param functionId A unique identifier for the function
     * @param name Display name for the function
     * @param description Description of what the function does
     * @param functionType Type of function (TAB, POPUP, FUNCTION)
     * @return True if the function was registered successfully
     */
    public boolean registerFunction(String functionId, String name, String description, 
                                   AIFunctionType functionType) {
        try {
            AIFunctionMetadata metadata = new AIFunctionMetadata(
                functionId, name, description, functionType);
            
            functionRegistry.put(functionId, metadata);
            
            // Initialize memory segment if it doesn't exist
            if (!memorySegments.containsKey(functionId)) {
                memorySegments.put(functionId, new ArrayList<>());
            }
            
            saveRegistry();
            return true;
        } catch (Exception e) {
            logger.error("Failed to register function {}", functionId, e);
            return false;
        }
    }
    
    /**
     * Updates function metadata.
     * 
     * @param functionId The function ID
     * @param name New name (or null to keep existing)
     * @param description New description (or null to keep existing)
     * @return True if the function was updated successfully
     */
    public boolean updateFunction(String functionId, String name, String description) {
        try {
            AIFunctionMetadata metadata = functionRegistry.get(functionId);
            if (metadata != null) {
                if (name != null) {
                    metadata.setName(name);
                }
                if (description != null) {
                    metadata.setDescription(description);
                }
                
                functionRegistry.put(functionId, metadata);
                saveRegistry();
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to update function {}", functionId, e);
            return false;
        }
    }
    
    /**
     * Gets a list of all registered functions.
     * 
     * @return List of all function metadata
     */
    public List<AIFunctionMetadata> getAllFunctions() {
        return new ArrayList<>(functionRegistry.values());
    }
    
    /**
     * Gets a function by ID.
     * 
     * @param functionId The function ID
     * @return The function metadata, or null if not found
     */
    public AIFunctionMetadata getFunction(String functionId) {
        return functionRegistry.get(functionId);
    }
    
    /**
     * Deletes a function and its memory.
     * 
     * @param functionId The function ID
     * @return True if the function was deleted successfully
     */
    public boolean deleteFunction(String functionId) {
        try {
            functionRegistry.remove(functionId);
            memorySegments.remove(functionId);
            
            // Delete memory file
            Path memoryPath = Paths.get(MEMORY_DIR, functionId + ".json");
            if (Files.exists(memoryPath)) {
                Files.delete(memoryPath);
            }
            
            saveRegistry();
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete function {}", functionId, e);
            return false;
        }
    }
    
    /**
     * Adds a message to a function's memory.
     * 
     * @param functionId The function ID
     * @param message The message to add
     * @return True if the message was added successfully
     */
    public boolean addMessage(String functionId, AIMessage message) {
        try {
            // Check if function exists, register it if not
            if (!functionRegistry.containsKey(functionId)) {
                registerFunction(functionId, functionId, "Auto-registered function", 
                    AIFunctionType.FUNCTION);
            }
            
            // Get memory segment
            List<AIMessage> messages = memorySegments.get(functionId);
            if (messages == null) {
                messages = new ArrayList<>();
                memorySegments.put(functionId, messages);
            }
            
            // Add message
            messages.add(message);
            
            // Save memory segment
            saveMemorySegment(functionId);
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to add message to function {}", functionId, e);
            return false;
        }
    }
    
    /**
     * Gets all messages for a function.
     * 
     * @param functionId The function ID
     * @return List of messages, or empty list if function not found
     */
    public List<AIMessage> getMessages(String functionId) {
        List<AIMessage> messages = memorySegments.get(functionId);
        if (messages == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(messages);
    }
    
    /**
     * Clears all messages for a function.
     * 
     * @param functionId The function ID
     * @return True if the messages were cleared successfully
     */
    public boolean clearMessages(String functionId) {
        try {
            memorySegments.put(functionId, new ArrayList<>());
            saveMemorySegment(functionId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to clear messages for function {}", functionId, e);
            return false;
        }
    }
    
    /**
     * Gets a summary of memory usage for all functions.
     * 
     * @return Map of function ID to message count
     */
    public Map<String, Integer> getMemoryUsage() {
        Map<String, Integer> usage = new HashMap<>();
        for (Map.Entry<String, List<AIMessage>> entry : memorySegments.entrySet()) {
            usage.put(entry.getKey(), entry.getValue().size());
        }
        return usage;
    }
    
    /**
     * Enum defining types of AI functions.
     */
    public enum AIFunctionType {
        TAB, POPUP, FUNCTION
    }
    
    /**
     * Class representing metadata about an AI function.
     */
    public static class AIFunctionMetadata {
        private String id;
        private String name;
        private String description;
        private AIFunctionType type;
        
        // For Jackson deserialization
        public AIFunctionMetadata() {}
        
        public AIFunctionMetadata(String id, String name, String description, AIFunctionType type) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public AIFunctionType getType() {
            return type;
        }
        
        public void setType(AIFunctionType type) {
            this.type = type;
        }
    }
} 