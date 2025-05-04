package com.spacedataarchive.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.TranslateTransition;
import javafx.util.Duration;

/**
 * Controller for the simple floating chat window.
 */
public class FloatingChatController {
    private static final Logger logger = LoggerFactory.getLogger(FloatingChatController.class);
    
    @FXML private VBox floatingChatContainer;
    @FXML private Button toggleButton;
    @FXML private ImageView aiIconView;
    @FXML private Label chatTitleLabel;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private Label statusLabel;
    @FXML private Button minimizeButton;
    @FXML private HBox chatHeader;
    
    private boolean isExpanded = false;
    private double dragStartX, dragStartY;
    private double expandedHeight;
    private double expandedWidth;
    private static final double ICON_SIZE = 56.0; // Size of the circular icon when collapsed
    
    /**
     * Initializes the controller.
     */
    public void initialize() {
        logger.info("Initializing FloatingChatController");
        
        // Load AI icon
        try {
            Image aiIcon = new Image(getClass().getResourceAsStream("/images/ai-icon.png"));
            if (aiIcon.isError()) {
                // Try SVG as fallback
                aiIcon = new Image(getClass().getResourceAsStream("/images/ai-icon.svg"));
                if (aiIcon.isError()) {
                    // Fallback to text if both fail
                    logger.warn("AI icon not found, using fallback text");
                    toggleButton.setText("AI");
                    aiIconView.setVisible(false);
                } else {
                    aiIconView.setImage(aiIcon);
                }
            } else {
                aiIconView.setImage(aiIcon);
            }
        } catch (Exception e) {
            logger.warn("Failed to load AI icon", e);
            toggleButton.setText("AI");
            aiIconView.setVisible(false);
        }
        
        // Setup message input
        messageInput.setWrapText(true);
        messageInput.setOnKeyPressed(event -> {
            if (event.isShiftDown() && event.getCode().toString().equals("ENTER")) {
                messageInput.appendText("\n");
                event.consume();
            } else if (event.getCode().toString().equals("ENTER")) {
                sendMessage();
                event.consume();
            }
        });
        
        // Setup send button
        sendButton.setOnAction(event -> sendMessage());
        
        // Setup toggle button and minimize button
        toggleButton.setOnAction(event -> toggleChatWindow());
        minimizeButton.setOnAction(event -> collapseChatWindow());
        
        // Setup drag functionality
        setupDraggable();
        
        // Initialize sizes and state
        Platform.runLater(() -> {
            // Save the initial expanded dimensions
            expandedHeight = floatingChatContainer.getPrefHeight();
            expandedWidth = floatingChatContainer.getPrefWidth();
            
            // Initially collapsed (just the icon)
            collapseChatWindow();
            
            // Add welcome messages for when expanded
            addSystemMessage("Welcome to the Space Data Archive AI Assistant");
            addSystemMessage("How can I help you with space data today?");
        });
    }
    
    /**
     * Sets up draggable behavior for the floating chat window.
     */
    private void setupDraggable() {
        chatHeader.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });
        
        chatHeader.setOnMouseDragged(event -> {
            floatingChatContainer.setTranslateX(floatingChatContainer.getTranslateX() + event.getSceneX() - dragStartX);
            floatingChatContainer.setTranslateY(floatingChatContainer.getTranslateY() + event.getSceneY() - dragStartY);
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });
        
        // Make the chat header visually indicate it's draggable
        chatHeader.setCursor(javafx.scene.Cursor.MOVE);
    }
    
    /**
     * Toggles the chat window between expanded and collapsed states.
     */
    private void toggleChatWindow() {
        if (isExpanded) {
            collapseChatWindow();
        } else {
            expandChatWindow();
        }
    }
    
    /**
     * Expands the chat window.
     */
    private void expandChatWindow() {
        isExpanded = true;
        
        // Show chat elements
        chatTitleLabel.setVisible(true);
        chatScrollPane.setVisible(true);
        messageInput.setVisible(true);
        sendButton.setVisible(true);
        statusLabel.setVisible(true);
        minimizeButton.setVisible(true);
        
        // Restore expanded dimensions
        floatingChatContainer.setPrefHeight(expandedHeight);
        floatingChatContainer.setPrefWidth(expandedWidth);
        floatingChatContainer.setMaxHeight(expandedHeight);
        floatingChatContainer.setMinHeight(chatHeader.getPrefHeight());
        floatingChatContainer.setMaxWidth(expandedWidth);
        floatingChatContainer.setMinWidth(expandedWidth);
        
        // Style adjustments
        floatingChatContainer.getStyleClass().remove("floating-chat-collapsed");
        chatHeader.getStyleClass().remove("floating-chat-header-collapsed");
        
        // Remove any transform
        floatingChatContainer.setTranslateY(0);
        
        // Scroll to bottom of chat
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Collapses the chat window to just an icon.
     */
    private void collapseChatWindow() {
        isExpanded = false;
        
        // Hide chat elements
        chatTitleLabel.setVisible(false);
        chatScrollPane.setVisible(false);
        messageInput.setVisible(false);
        sendButton.setVisible(false);
        statusLabel.setVisible(false);
        minimizeButton.setVisible(false);
        
        // Set to icon-only size (circular)
        floatingChatContainer.setPrefHeight(ICON_SIZE);
        floatingChatContainer.setMaxHeight(ICON_SIZE);
        floatingChatContainer.setMinHeight(ICON_SIZE);
        floatingChatContainer.setPrefWidth(ICON_SIZE);
        floatingChatContainer.setMaxWidth(ICON_SIZE);
        floatingChatContainer.setMinWidth(ICON_SIZE);
        
        // Style adjustments for circular appearance
        floatingChatContainer.getStyleClass().add("floating-chat-collapsed");
        chatHeader.getStyleClass().add("floating-chat-header-collapsed");
        
        // No translation needed since we're changing actual size
        floatingChatContainer.setTranslateY(0);
    }
    
    /**
     * Sends a message.
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
        
        // Simulate AI response (in real implementation, this would call your AI service)
        simulateAIResponse(message);
    }
    
    /**
     * Adds a user message to the chat UI.
     */
    private void addUserMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 5, 5, 10));
        
        TextFlow messageFlow = new TextFlow();
        messageFlow.getStyleClass().add("user-message");
        messageFlow.setPadding(new Insets(8));
        
        Text text = new Text(message);
        messageFlow.getChildren().add(text);
        
        messageBox.getChildren().add(messageFlow);
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Adds an AI assistant message to the chat UI.
     */
    private void addAIMessage(String message) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 10, 5, 5));
        
        TextFlow messageFlow = new TextFlow();
        messageFlow.getStyleClass().add("ai-message");
        messageFlow.setPadding(new Insets(8));
        
        Text text = new Text(message);
        messageFlow.getChildren().add(text);
        
        messageBox.getChildren().add(messageFlow);
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to bottom
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
    
    /**
     * Adds a system message to the chat UI.
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
     * Simulates an AI response based on the user's message.
     * In a real implementation, this would call your AI service.
     */
    private void simulateAIResponse(String userMessage) {
        // Simple responses for demo purposes
        String response;
        userMessage = userMessage.toLowerCase();
        
        if (userMessage.contains("hello") || userMessage.contains("hi")) {
            response = "Hello! How can I help you with the space data archive today?";
        } else if (userMessage.contains("help")) {
            response = "I can help you with browsing space data, filtering results, and understanding archive features. What would you like to know about?";
        } else if (userMessage.contains("search") || userMessage.contains("find")) {
            response = "To search for specific data, you can use the Search & Access tab. You can filter by URL, title, or other metadata.";
        } else if (userMessage.contains("theme")) {
            response = "You can toggle between light and dark themes using the theme button in the top right corner of the application.";
        } else {
            response = "I understand you're asking about '" + userMessage + "'. Please check the documentation or try a more specific question about the space data archive.";
        }
        
        // Add typing indicator
        addSystemMessage("AI Assistant is typing...");
        
        // Simulate typing delay
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    // Remove typing indicator
                    chatContainer.getChildren().remove(chatContainer.getChildren().size() - 1);
                    // Add AI response
                    addAIMessage(response);
                });
            } catch (InterruptedException e) {
                logger.error("Error simulating AI response", e);
            }
        }).start();
    }
} 