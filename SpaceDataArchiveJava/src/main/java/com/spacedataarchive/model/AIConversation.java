package com.spacedataarchive.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an AI conversation with messages from user and assistant.
 */
public class AIConversation {
    private String id;
    private String title;
    private List<AIMessage> messages;
    private Instant createdAt;
    private Instant updatedAt;
    private String functionId; // ID of the AI function this conversation belongs to
    
    /**
     * Default constructor.
     */
    public AIConversation() {
        this.id = UUID.randomUUID().toString();
        this.title = "New Conversation";
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    /**
     * Constructor with ID, title, and timestamps.
     * 
     * @param id The ID of the conversation
     * @param title The title of the conversation
     * @param createdAt When the conversation was created
     * @param updatedAt When the conversation was last updated
     */
    public AIConversation(String id, String title, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.messages = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Adds a message to the conversation.
     * 
     * @param message The message to add
     */
    public void addMessage(AIMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        
        messages.add(message);
        
        // If this is the first response from the assistant and title is default, set a better title
        if (messages.size() == 2 && message.isFromAssistant() && "New Conversation".equals(title)) {
            // Get the first 30 characters of the message or less
            String content = message.getContent();
            if (content.length() > 30) {
                content = content.substring(0, 27) + "...";
            }
            this.title = content;
        }
    }
    
    /**
     * Gets the ID of the conversation.
     * 
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the ID of the conversation.
     * 
     * @param id The ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the title of the conversation.
     * 
     * @return The title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the title of the conversation.
     * 
     * @param title The title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Gets the messages in the conversation.
     * 
     * @return The list of messages
     */
    public List<AIMessage> getMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }
    
    /**
     * Sets the messages in the conversation.
     * 
     * @param messages The list of messages
     */
    public void setMessages(List<AIMessage> messages) {
        this.messages = messages;
    }
    
    /**
     * Gets when the conversation was created.
     * 
     * @return The creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets when the conversation was created.
     * 
     * @param createdAt The creation time
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets when the conversation was last updated.
     * 
     * @return The update time
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets when the conversation was last updated.
     * 
     * @param updatedAt The update time
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the ID of the AI function this conversation belongs to.
     * 
     * @return The function ID, or null if not assigned to a function
     */
    public String getFunctionId() {
        return functionId;
    }
    
    /**
     * Sets the ID of the AI function this conversation belongs to.
     * 
     * @param functionId The function ID
     */
    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }
    
    /**
     * Gets a formatted timestamp for the creation time.
     * 
     * @return The formatted creation time
     */
    @JsonIgnore
    public String getFormattedCreatedAt() {
        return createdAt.toString();
    }
    
    /**
     * Gets a formatted timestamp for the update time.
     * 
     * @return The formatted update time
     */
    @JsonIgnore
    public String getFormattedUpdatedAt() {
        return updatedAt.toString();
    }
} 