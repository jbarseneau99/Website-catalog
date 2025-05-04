package com.spacedataarchive.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spacedataarchive.model.AIConversation;
import com.spacedataarchive.model.AIMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Service for managing AI conversations.
 */
public class ConversationService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);
    private static final String CONVERSATIONS_DIR = "data/conversations";
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    
    private final AnthropicAIService aiService;
    private final AIMemoryService memoryService;
    private ConcurrentHashMap<String, AIConversation> conversations = new ConcurrentHashMap<>();
    
    /**
     * Constructor that initializes the conversation service.
     * 
     * @param aiService The AI service to use for sending messages
     */
    public ConversationService(AnthropicAIService aiService) {
        this.aiService = aiService;
        this.memoryService = new AIMemoryService();
        initialize();
    }
    
    /**
     * Constructor that initializes the conversation service with a memory service.
     * 
     * @param aiService The AI service to use for sending messages
     * @param memoryService The memory service to use for storing segmented conversations
     */
    public ConversationService(AnthropicAIService aiService, AIMemoryService memoryService) {
        this.aiService = aiService;
        this.memoryService = memoryService;
        initialize();
    }
    
    /**
     * Initializes the conversation service.
     */
    private void initialize() {
        try {
            Path conversationsDir = Paths.get(CONVERSATIONS_DIR);
            if (!Files.exists(conversationsDir)) {
                Files.createDirectories(conversationsDir);
            }
            loadConversations();
        } catch (IOException e) {
            logger.error("Failed to initialize conversations directory", e);
        }
    }
    
    /**
     * Loads conversations from disk.
     */
    private void loadConversations() {
        File dir = new File(CONVERSATIONS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                try {
                    AIConversation conversation = mapper.readValue(file, AIConversation.class);
                    conversations.put(conversation.getId(), conversation);
                } catch (IOException e) {
                    logger.error("Failed to load conversation from file: {}", file.getPath(), e);
                    // Try to handle the file if it's corrupted due to date format issues
                    try {
                        // Delete the problematic file to prevent future errors
                        boolean deleted = file.delete();
                        if (deleted) {
                            logger.info("Deleted corrupted conversation file: {}", file.getPath());
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to delete corrupted conversation file: {}", file.getPath(), ex);
                    }
                }
            }
        }
        
        logger.info("Loaded {} conversations", conversations.size());
    }
    
    /**
     * Creates a new conversation.
     * 
     * @return The newly created conversation
     */
    public AIConversation createConversation() {
        return createConversation("New Conversation");
    }
    
    /**
     * Creates a new conversation with a specific title.
     * 
     * @param title The title of the conversation
     * @return The newly created conversation
     */
    public AIConversation createConversation(String title) {
        return createConversation(title, null);
    }
    
    /**
     * Creates a new conversation with a specific title and function ID.
     * 
     * @param title The title of the conversation
     * @param functionId The ID of the AI function using this conversation
     * @return The newly created conversation
     */
    public AIConversation createConversation(String title, String functionId) {
        String id = generateId();
        
        Instant now = Instant.now();
        AIConversation conversation = new AIConversation(id, title, now, now);
        
        // Register with memory service if functionId is provided
        if (functionId != null) {
            conversation.setFunctionId(functionId);
            memoryService.registerFunction(
                functionId,
                "Function: " + functionId,
                "AI conversation function",
                AIMemoryService.AIFunctionType.FUNCTION
            );
        }
        
        conversations.put(id, conversation);
        saveConversation(conversation);
        
        return conversation;
    }
    
    /**
     * Gets a conversation by ID.
     * 
     * @param id The ID of the conversation
     * @return The conversation, or null if not found
     */
    public AIConversation getConversation(String id) {
        return conversations.get(id);
    }
    
    /**
     * Gets all conversations.
     * 
     * @return List of all conversations
     */
    public List<AIConversation> getAllConversations() {
        return new ArrayList<>(conversations.values());
    }
    
    /**
     * Adds a message to a conversation.
     * 
     * @param conversationId The ID of the conversation
     * @param message The message to add
     * @return The updated conversation, or null if the conversation doesn't exist
     */
    public AIConversation addMessage(String conversationId, AIMessage message) {
        AIConversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.addMessage(message);
            conversation.setUpdatedAt(Instant.now());
            saveConversation(conversation);
            
            // If this conversation has a functionId, also store in memory service
            if (conversation.getFunctionId() != null) {
                memoryService.addMessage(conversation.getFunctionId(), message);
            }
            
            return conversation;
        }
        return null;
    }
    
    /**
     * Sends a message to the AI and adds both the user message and AI response to the conversation.
     * 
     * @param conversationId The ID of the conversation
     * @param userMessage The user's message
     * @return The AI's response message
     */
    public AIMessage sendMessage(String conversationId, String userMessage) {
        AIConversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            return null;
        }
        
        // Create and add user message
        AIMessage userMsg = new AIMessage(AIMessage.Role.USER, userMessage);
        conversation.addMessage(userMsg);
        
        // If this conversation has a functionId, also store in memory service
        if (conversation.getFunctionId() != null) {
            memoryService.addMessage(conversation.getFunctionId(), userMsg);
        }
        
        // Prepare context for AI (include previous messages)
        String prompt = userMessage;
        
        // Get AI response
        String aiResponse = aiService.sendMessage(prompt);
        
        // Create and add AI message
        AIMessage aiMessage = new AIMessage(AIMessage.Role.ASSISTANT, aiResponse);
        conversation.addMessage(aiMessage);
        conversation.setUpdatedAt(Instant.now());
        
        // If this conversation has a functionId, also store in memory service
        if (conversation.getFunctionId() != null) {
            memoryService.addMessage(conversation.getFunctionId(), aiMessage);
        }
        
        saveConversation(conversation);
        
        return aiMessage;
    }
    
    /**
     * Sends a message to the AI with streaming response and adds both the user message and AI response to the conversation.
     * 
     * @param conversationId The ID of the conversation
     * @param userMessage The user's message
     * @param responseHandler Handler for the streaming response
     */
    public void sendMessageStreaming(String conversationId, String userMessage, Consumer<String> responseHandler) {
        AIConversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            return;
        }
        
        // Create and add user message
        AIMessage userMsg = new AIMessage(AIMessage.Role.USER, userMessage);
        conversation.addMessage(userMsg);
        
        // If this conversation has a functionId, also store in memory service
        if (conversation.getFunctionId() != null) {
            memoryService.addMessage(conversation.getFunctionId(), userMsg);
        }
        
        // Prepare context for AI (include previous messages)
        String prompt = userMessage;
        
        // StringBuilder to accumulate streaming response
        StringBuilder fullResponse = new StringBuilder();
        
        // Get AI response (streaming)
        aiService.sendMessageStreaming(prompt, chunk -> {
            // Add chunk to the accumulated response
            fullResponse.append(chunk);
            
            // Pass chunk to the handler
            responseHandler.accept(chunk);
        });
        
        // Create and add AI message (will be updated with the full response when streaming is complete)
        AIMessage aiMessage = new AIMessage(AIMessage.Role.ASSISTANT, "");
        conversation.addMessage(aiMessage);
        conversation.setUpdatedAt(Instant.now());
        
        // Schedule a task to update the AI message with the full response after streaming is complete
        new Thread(() -> {
            try {
                // Wait a bit to ensure streaming is complete
                Thread.sleep(1000);
                
                // Update AI message with full response
                aiMessage.setContent(fullResponse.toString());
                
                // If this conversation has a functionId, also store in memory service
                if (conversation.getFunctionId() != null) {
                    memoryService.addMessage(conversation.getFunctionId(), aiMessage);
                }
                
                saveConversation(conversation);
            } catch (InterruptedException e) {
                logger.error("Failed to update AI message with full response", e);
            }
        }).start();
    }
    
    /**
     * Updates a conversation's title.
     * 
     * @param conversationId The ID of the conversation
     * @param title The new title
     * @return The updated conversation, or null if the conversation doesn't exist
     */
    public AIConversation updateTitle(String conversationId, String title) {
        AIConversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversation.setUpdatedAt(Instant.now());
            saveConversation(conversation);
            return conversation;
        }
        return null;
    }
    
    /**
     * Deletes a conversation.
     * 
     * @param conversationId The ID of the conversation to delete
     * @return True if the conversation was deleted, false otherwise
     */
    public boolean deleteConversation(String conversationId) {
        AIConversation conversation = conversations.remove(conversationId);
        if (conversation != null) {
            try {
                Files.deleteIfExists(Paths.get(CONVERSATIONS_DIR, "conv-" + conversationId + ".json"));
                
                // If this conversation has a functionId, also clear from memory service
                if (conversation.getFunctionId() != null) {
                    memoryService.clearMessages(conversation.getFunctionId());
                }
                
                return true;
            } catch (IOException e) {
                logger.error("Failed to delete conversation file", e);
                return false;
            }
        }
        return false;
    }
    
    /**
     * Sets a function ID for a conversation to enable memory segmentation.
     * 
     * @param conversationId The ID of the conversation
     * @param functionId The function ID
     * @return The updated conversation, or null if the conversation doesn't exist
     */
    public AIConversation setFunctionId(String conversationId, String functionId) {
        AIConversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.setFunctionId(functionId);
            
            // Register with memory service
            memoryService.registerFunction(
                functionId,
                "Function: " + functionId,
                "AI conversation function",
                AIMemoryService.AIFunctionType.FUNCTION
            );
            
            // Sync existing messages to memory service
            for (AIMessage message : conversation.getMessages()) {
                memoryService.addMessage(functionId, message);
            }
            
            saveConversation(conversation);
            return conversation;
        }
        return null;
    }
    
    /**
     * Gets the memory service used by this conversation service.
     * 
     * @return The memory service
     */
    public AIMemoryService getMemoryService() {
        return memoryService;
    }
    
    /**
     * Saves a conversation to disk.
     * 
     * @param conversation The conversation to save
     */
    private void saveConversation(AIConversation conversation) {
        try {
            File file = new File(CONVERSATIONS_DIR, "conv-" + conversation.getId() + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, conversation);
        } catch (IOException e) {
            logger.error("Failed to save conversation", e);
        }
    }
    
    /**
     * Generates a unique ID for a new conversation.
     * 
     * @return A unique ID
     */
    private String generateId() {
        return String.valueOf(System.currentTimeMillis());
    }
} 