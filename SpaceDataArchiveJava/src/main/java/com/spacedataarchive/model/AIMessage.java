package com.spacedataarchive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a message in an AI conversation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIMessage {
    
    /**
     * Enum representing the role of the message sender.
     */
    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
    
    private String id;
    private Role role;
    private String content;
    private LocalDateTime timestamp;
    
    /**
     * Default constructor.
     */
    public AIMessage() {
        this.id = generateId();
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Constructor with role and content.
     * 
     * @param role The role of the message sender
     * @param content The message content
     */
    public AIMessage(Role role, String content) {
        this.id = generateId();
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Generates a unique ID for the message.
     * 
     * @return A unique ID
     */
    private String generateId() {
        return "msg-" + System.currentTimeMillis();
    }
    
    /**
     * Gets the ID.
     * 
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the ID.
     * 
     * @param id The ID to set
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the role.
     * 
     * @return The role
     */
    public Role getRole() {
        return role;
    }
    
    /**
     * Sets the role.
     * 
     * @param role The role to set
     */
    public void setRole(Role role) {
        this.role = role;
    }
    
    /**
     * Gets the content.
     * 
     * @return The content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Sets the content.
     * 
     * @param content The content to set
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * Gets the timestamp.
     * 
     * @return The timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Sets the timestamp.
     * 
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Checks if the message is from the user.
     * 
     * @return True if the message is from the user
     */
    public boolean isFromUser() {
        return role == Role.USER;
    }
    
    /**
     * Checks if the message is from the assistant.
     * 
     * @return True if the message is from the assistant
     */
    public boolean isFromAssistant() {
        return role == Role.ASSISTANT;
    }
    
    /**
     * Checks if the message is from the system.
     * 
     * @return True if the message is from the system
     */
    public boolean isFromSystem() {
        return role == Role.SYSTEM;
    }
    
    /**
     * Gets a formatted timestamp.
     * 
     * @return The formatted timestamp
     */
    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return timestamp.format(formatter);
    }
    
    /**
     * Gets whether this message is from the user.
     * This is a legacy property for backward compatibility.
     * 
     * @return True if from user
     */
    @JsonProperty("fromUser")
    public Boolean getFromUser() {
        return isFromUser();
    }
    
    /**
     * Sets whether this message is from the user.
     * This is a legacy property for backward compatibility.
     * 
     * @param fromUser Whether this message is from the user
     */
    @JsonProperty("fromUser")
    public void setFromUser(Boolean fromUser) {
        if (fromUser != null && fromUser) {
            this.role = Role.USER;
        } else if (fromUser != null && !fromUser) {
            this.role = Role.ASSISTANT;
        }
    }
    
    @Override
    public String toString() {
        return "AIMessage{" +
                "id='" + id + '\'' +
                ", role=" + role +
                ", content='" + (content != null && content.length() > 20 ? content.substring(0, 17) + "..." : content) + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 