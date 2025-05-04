package com.spacedataarchive.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spacedataarchive.service.ai.AIServiceManager;
import com.spacedataarchive.service.ai.AnthropicAIService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the AI Chat functionality using ControlsFX PopOver
 */
public class AIChatController {
    private static final Logger logger = LoggerFactory.getLogger(AIChatController.class);
    
    private final PopOver chatPopOver;
    private final VBox chatContainer;
    private final TextArea messageInput;
    private final ScrollPane chatScrollPane;
    private final Button sendButton;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    
    private Button triggerButton;
    private AnthropicAIService aiService;
    
    /**
     * Creates a new AI Chat Controller
     */
    public AIChatController() {
        // Initialize AI Service
        this.aiService = AIServiceManager.getInstance().getAiService();
        
        // Create chat components
        chatContainer = new VBox(10);
        chatContainer.getStyleClass().add("ai-chat-messages");
        chatContainer.setPadding(new Insets(15));
        
        chatScrollPane = new ScrollPane(chatContainer);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(300);
        chatScrollPane.getStyleClass().add("ai-chat-scroll");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        
        messageInput = new TextArea();
        messageInput.setPromptText("Type your message here...");
        messageInput.setPrefRowCount(3);
        messageInput.setWrapText(true);
        messageInput.getStyleClass().add("ai-chat-input");
        
        sendButton = new Button("Send");
        sendButton.getStyleClass().add("ai-chat-send");
        
        HBox inputContainer = new HBox(10, messageInput, sendButton);
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.setPadding(new Insets(10));
        inputContainer.getStyleClass().add("ai-chat-input-container");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        // Create root container
        VBox root = new VBox();
        root.getChildren().addAll(createHeader(), chatScrollPane, inputContainer);
        root.setPrefWidth(350);
        root.setPrefHeight(450);
        root.getStyleClass().add("ai-chat-container");
        
        // Configure PopOver
        chatPopOver = new PopOver(root);
        chatPopOver.setTitle("AI Assistant");
        chatPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        chatPopOver.setAutoHide(false);
        chatPopOver.setAnimated(true);
        chatPopOver.setHeaderAlwaysVisible(true);
        chatPopOver.getStyleClass().add("ai-chat-popover");
        
        // Set up event handlers
        setupEventHandlers();
        
        // Add welcome messages
        Platform.runLater(() -> {
            // Check if AI service is initialized
            if (aiService != null && aiService.isInitialized()) {
                addSystemMessage("Welcome to the Space Data Archive AI Assistant");
                addSystemMessage("How can I help you with space data today?");
            } else {
                addSystemMessage("Welcome to the Space Data Archive AI Assistant");
                addSystemMessage("AI service is not properly configured. Please check your API key.");
            }
        });
        
        logger.info("AI Chat Controller initialized");
    }
    
    /**
     * Create the header for the chat
     */
    private Node createHeader() {
        Label titleLabel = new Label("AI Assistant");
        titleLabel.getStyleClass().add("ai-chat-title");
        
        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("ai-chat-close");
        closeButton.setOnAction(e -> chatPopOver.hide());
        
        HBox header = new HBox(10, titleLabel, closeButton);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("ai-chat-header");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        return header;
    }
    
    /**
     * Set up event handlers for the chat components
     */
    private void setupEventHandlers() {
        // Handle Enter key in message input
        messageInput.setOnKeyPressed(event -> {
            if (event.isShiftDown() && event.getCode().toString().equals("ENTER")) {
                messageInput.appendText("\n");
                event.consume();
            } else if (event.getCode().toString().equals("ENTER")) {
                sendMessage();
                event.consume();
            }
        });
        
        // Handle send button click
        sendButton.setOnAction(event -> sendMessage());
    }
    
    /**
     * Show the chat popup
     * @param owner The node that triggers the popup
     */
    public void show(Node owner) {
        this.triggerButton = (Button) owner;
        chatPopOver.show(owner);
        
        // Focus the input field
        Platform.runLater(() -> messageInput.requestFocus());
    }
    
    /**
     * Hide the chat popup
     */
    public void hide() {
        chatPopOver.hide();
    }
    
    /**
     * Check if the chat popup is showing
     * @return true if showing
     */
    public boolean isShowing() {
        return chatPopOver.isShowing();
    }
    
    /**
     * Toggle the chat popup
     * @param owner The node that triggers the popup
     */
    public void toggle(Node owner) {
        if (chatPopOver.isShowing()) {
            hide();
        } else {
            show(owner);
        }
    }
    
    /**
     * Send a message from the user
     */
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Clear the input field
        messageInput.clear();
        
        // Add user message to UI
        addUserMessage(message);
        
        // Disable input while processing
        messageInput.setDisable(true);
        sendButton.setDisable(true);
        
        // Add typing indicator
        addSystemMessage("AI Assistant is typing...");
        
        // Process message in background
        processMessageInBackground(message);
    }
    
    /**
     * Process the message in a background thread
     * @param message The user's message
     */
    private void processMessageInBackground(String message) {
        CompletableFuture.runAsync(() -> {
            String response;
            
            try {
                // Check if AI service is available
                if (aiService != null && aiService.isInitialized()) {
                    // Get response from AI service
                    response = aiService.sendMessage(message);
                } else {
                    // Fallback to simulated response
                    response = getSimulatedResponse(message);
                }
            } catch (Exception e) {
                logger.error("Error getting AI response", e);
                response = "Sorry, I encountered an error processing your request. Please try again later.";
            }
            
            // Need final variable for lambda
            final String finalResponse = response;
            
            // Update UI on JavaFX thread
            Platform.runLater(() -> {
                // Remove typing indicator
                chatContainer.getChildren().remove(chatContainer.getChildren().size() - 1);
                
                // Add AI response
                addAIMessage(finalResponse);
                
                // Re-enable input
                messageInput.setDisable(false);
                sendButton.setDisable(false);
                messageInput.requestFocus();
            });
        });
    }
    
    /**
     * Get a simulated response when AI service is unavailable
     * @param userMessage The user's message
     * @return A simulated response
     */
    private String getSimulatedResponse(String userMessage) {
        // Simple responses for demo purposes
        userMessage = userMessage.toLowerCase();
        
        if (userMessage.contains("hello") || userMessage.contains("hi")) {
            return "Hello! How can I help you with the space data archive today?";
        } else if (userMessage.contains("help")) {
            return "I can help you with browsing space data, filtering results, and understanding archive features. What would you like to know about?";
        } else if (userMessage.contains("search") || userMessage.contains("find")) {
            return "To search for specific data, you can use the Search & Access tab. You can filter by URL, title, or other metadata.";
        } else if (userMessage.contains("theme")) {
            return "You can toggle between light and dark themes using the theme button in the top right corner of the application.";
        } else {
            return "I understand you're asking about '" + userMessage + "'. Please check the documentation or try a more specific question about the space data archive.";
        }
    }
    
    /**
     * Add a user message to the chat
     * @param message The message text
     */
    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 5, 5, 10));
        
        TextFlow messageFlow = new TextFlow();
        messageFlow.getStyleClass().add("user-message");
        messageFlow.setPadding(new Insets(12));
        
        Text text = new Text(message);
        messageFlow.getChildren().add(text);
        
        messageBox.getChildren().add(messageFlow);
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Add an AI message to the chat
     * @param message The message text
     */
    private void addAIMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 10, 5, 5));
        
        TextFlow messageFlow = new TextFlow();
        messageFlow.getStyleClass().add("ai-message");
        messageFlow.setPadding(new Insets(12));
        
        Text text = new Text(message);
        messageFlow.getChildren().add(text);
        
        messageBox.getChildren().add(messageFlow);
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Add a system message to the chat
     * @param message The message text
     */
    private void addSystemMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPadding(new Insets(5));
        
        Label systemLabel = new Label(message);
        systemLabel.getStyleClass().add("system-message");
        systemLabel.setWrapText(true);
        
        messageBox.getChildren().add(systemLabel);
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Clean up resources when no longer needed
     */
    public void shutdown() {
        executorService.shutdown();
    }
} 