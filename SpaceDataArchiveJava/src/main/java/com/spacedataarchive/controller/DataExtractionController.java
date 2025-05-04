package com.spacedataarchive.controller;

import com.spacedataarchive.model.AIMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Data Extraction tab.
 * Demonstrates AI functionality integrated with data extraction.
 */
public class DataExtractionController {
    private static final Logger logger = LoggerFactory.getLogger(DataExtractionController.class);
    
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;
    @FXML private Button extractDataButton;
    @FXML private ComboBox<String> extractionMethodComboBox;
    @FXML private ListView<String> urlsListView;
    
    /**
     * Toggles the AI assistant drawer visibility
     */
    @FXML
    public void toggleAIDrawer() {
        if (aiAssistantDrawer != null && aiAssistantToggle != null) {
            boolean isDrawerVisible = aiAssistantDrawer.isVisible();
            
            // Toggle visibility
            aiAssistantDrawer.setVisible(!isDrawerVisible);
            aiAssistantToggle.setVisible(isDrawerVisible);
            
            // Log the action
            logger.info("Data extraction AI assistant drawer " + (isDrawerVisible ? "hidden" : "shown"));
        }
    }
    
    // Add field declarations for the drawer components and new UI elements
    @FXML private VBox aiAssistantDrawer;
    @FXML private VBox aiAssistantToggle;
    @FXML private Button toggleAIDrawerButton;
    @FXML private Button expandAIDrawerButton;
    @FXML private TableView<Object> extractedDataTable;
    @FXML private TableColumn<Object, String> urlColumn;
    @FXML private TableColumn<Object, String> titleColumn;
    @FXML private TableColumn<Object, String> typeColumn;
    @FXML private TableColumn<Object, String> sizeColumn;
    @FXML private TableColumn<Object, String> extractedAtColumn;
    @FXML private TextArea previewTextArea;
    @FXML private Pagination resultsPagination;
    @FXML private ComboBox<String> sourceComboBox;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing DataExtractionController");
        
        // Initialize UI components
        initializeUI();
        
        // Add placeholder conversation to demonstrate AI functionality
        addPlaceholderConversation();
        
        updateStatus("Data Extraction tab ready");
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        // Configure message input
        messageInput.setWrapText(true);
        messageInput.setPromptText("Ask AI about data extraction...");
        
        // Configure send button
        sendButton.setOnAction(e -> sendMessage());
        
        // Configure extraction method combo box
        extractionMethodComboBox.getItems().addAll(
            "Basic Text Extraction",
            "Structured Data Extraction",
            "Table Recognition",
            "AI-assisted Extraction"
        );
        extractionMethodComboBox.getSelectionModel().selectFirst();
        
        // Configure extract data button
        extractDataButton.setOnAction(e -> extractData());
        
        // Add some URLs to the list
        urlsListView.getItems().addAll(
            "https://spacenews.com/article1",
            "https://spacenews.com/article2",
            "https://spacenews.com/missions/mars-exploration"
        );
    }
    
    /**
     * Sends a message to the AI assistant.
     */
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        // Add user message to UI
        addMessageToUI(new AIMessage(AIMessage.Role.USER, message));
        
        // Clear input
        messageInput.clear();
        
        // Disable send button to prevent multiple requests
        sendButton.setDisable(true);
        
        // Simulate processing delay
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                
                // Generate a simple response
                String response = "I can help you extract structured data from these articles. " +
                                  "I've identified tables, charts, and key entities that can be extracted. " +
                                  "Would you like me to process all URLs or focus on a specific article?";
                
                Platform.runLater(() -> {
                    // Add AI response to UI
                    addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, response));
                    
                    // Re-enable send button
                    sendButton.setDisable(false);
                });
            } catch (InterruptedException e) {
                logger.error("Message processing interrupted", e);
                Platform.runLater(() -> sendButton.setDisable(false));
            }
        }).start();
    }
    
    /**
     * Extracts data from the selected URLs.
     */
    private void extractData() {
        String method = extractionMethodComboBox.getValue();
        List<String> selectedUrls = new ArrayList<>(urlsListView.getSelectionModel().getSelectedItems());
        
        if (selectedUrls.isEmpty()) {
            updateStatus("No URLs selected for extraction");
            return;
        }
        
        updateStatus("Extracting data using " + method + "...");
        
        // If AI-assisted extraction is selected
        if (method.equals("AI-assisted Extraction")) {
            // Add AI message to the chat
            String aiPrompt = "I'll extract data from " + selectedUrls.size() + " URLs using advanced " +
                             "entity recognition and structured data parsing. Processing...";
            addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, aiPrompt));
            
            // Simulate processing
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    
                    Platform.runLater(() -> {
                        String result = "Extraction complete! Found:\n" +
                                       "- 12 data tables\n" +
                                       "- 8 charts with numerical data\n" +
                                       "- 45 named entities (organizations, people, locations)\n" +
                                       "- 3 research methodologies\n\n" +
                                       "Would you like me to prepare this data for analysis or export it to a specific format?";
                        
                        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, result));
                        updateStatus("AI-assisted extraction completed");
                    });
                    
                } catch (InterruptedException e) {
                    logger.error("Extraction processing interrupted", e);
                    Platform.runLater(() -> updateStatus("Extraction failed"));
                }
            }).start();
        } else {
            // For other methods, just update status
            updateStatus("Extracted data from " + selectedUrls.size() + " URLs using " + method);
        }
    }
    
    /**
     * Adds a placeholder conversation to demonstrate AI functionality.
     */
    private void addPlaceholderConversation() {
        // Add a welcome message
        addMessageToUI(new AIMessage(AIMessage.Role.SYSTEM, 
            "Welcome to the Data Extraction AI Assistant. I can help you extract structured data " +
            "from web pages and organize it for analysis."
        ));
        
        // Add a sample conversation
        addMessageToUI(new AIMessage(AIMessage.Role.USER, 
            "Extract key information from these scientific articles."
        ));
        
        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, 
            "I've identified the following data elements from the selected articles:\n\n" +
            "1. Research methodologies used in each paper\n" +
            "2. Statistical findings and numerical data\n" +
            "3. Primary conclusions and results\n\n" +
            "I've also detected 5 data tables and 3 charts that can be extracted as structured data. " +
            "Would you like me to extract all of these elements or focus on a specific type?"
        ));
    }
    
    /**
     * Adds a message to the chat UI.
     * 
     * @param message The message to add
     */
    private void addMessageToUI(AIMessage message) {
        // Create message container
        VBox messageBox = new VBox();
        messageBox.setPadding(new Insets(10));
        messageBox.setSpacing(5);
        messageBox.setMaxWidth(500);
        
        // Create header with role
        Label roleLabel = new Label(message.isFromUser() ? "You" : 
                                   message.isFromAssistant() ? "AI Assistant" : "System");
        roleLabel.setStyle("-fx-font-weight: bold;");
        
        // Create message content
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        
        // Add components to message box
        messageBox.getChildren().addAll(roleLabel, contentLabel);
        
        // Style based on message role
        if (message.isFromUser()) {
            messageBox.setBackground(new Background(new BackgroundFill(
                Color.web("#e1f5fe"), new CornerRadii(10), Insets.EMPTY
            )));
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            HBox wrapper = new HBox(messageBox);
            wrapper.setAlignment(Pos.CENTER_RIGHT);
            chatContainer.getChildren().add(wrapper);
        } else if (message.isFromAssistant()) {
            messageBox.setBackground(new Background(new BackgroundFill(
                Color.web("#f0e6f7"), new CornerRadii(10), Insets.EMPTY
            )));
            HBox wrapper = new HBox(messageBox);
            wrapper.setAlignment(Pos.CENTER_LEFT);
            chatContainer.getChildren().add(wrapper);
        } else {
            messageBox.setBackground(new Background(new BackgroundFill(
                Color.web("#f5f5f5"), new CornerRadii(10), Insets.EMPTY
            )));
            messageBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 10;");
            HBox wrapper = new HBox(messageBox);
            wrapper.setAlignment(Pos.CENTER);
            chatContainer.getChildren().add(wrapper);
        }
        
        // Scroll to bottom
        chatScrollPane.setVvalue(1.0);
    }
    
    /**
     * Updates the status label.
     * 
     * @param status The status message
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
        logger.info(status);
    }
} 