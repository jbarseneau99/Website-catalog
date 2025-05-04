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
 * Controller for the Analysis tab.
 * Demonstrates AI functionality for data analysis.
 */
public class AnalysisController {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
    
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;
    @FXML private Button analyzeButton;
    @FXML private ComboBox<String> analysisTypeComboBox;
    @FXML private ListView<String> datasetListView;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing AnalysisController");
        
        // Initialize UI components
        initializeUI();
        
        // Add placeholder conversation to demonstrate AI functionality
        addPlaceholderConversation();
        
        updateStatus("Analysis tab ready");
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        // Configure message input
        messageInput.setWrapText(true);
        messageInput.setPromptText("Ask about data analysis...");
        
        // Configure send button
        sendButton.setOnAction(e -> sendMessage());
        
        // Configure analysis type combo box
        analysisTypeComboBox.getItems().addAll(
            "Trend Analysis",
            "Entity Recognition",
            "Topic Modeling",
            "Relationship Mapping"
        );
        analysisTypeComboBox.getSelectionModel().selectFirst();
        
        // Configure analyze button
        analyzeButton.setOnAction(e -> analyzeData());
        
        // Add sample datasets to the list
        datasetListView.getItems().addAll(
            "spacenews_articles.json",
            "nasa_press_releases.json",
            "space_mission_data.json"
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
                String response = "I can analyze this data to identify patterns and relationships. " +
                                  "I've found several notable trends in the space mission data: " +
                                  "1) Increasing commercial participation since 2015, " +
                                  "2) Shift toward smaller satellite deployments, " +
                                  "3) Growing focus on lunar missions. Would you like me to elaborate on any of these?";
                
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
     * Analyzes the selected datasets.
     */
    private void analyzeData() {
        String analysisType = analysisTypeComboBox.getValue();
        List<String> selectedDatasets = new ArrayList<>(datasetListView.getSelectionModel().getSelectedItems());
        
        if (selectedDatasets.isEmpty()) {
            updateStatus("No datasets selected for analysis");
            return;
        }
        
        updateStatus("Analyzing data using " + analysisType + "...");
        
        // If Relationship Mapping is selected
        if (analysisType.equals("Relationship Mapping")) {
            // Add AI message to the chat
            String aiPrompt = "I'll analyze the relationships between entities in " + selectedDatasets.size() + 
                              " datasets. Processing...";
            addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, aiPrompt));
            
            // Simulate processing
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    
                    Platform.runLater(() -> {
                        String result = "Analysis complete! I've mapped key relationships:\n\n" +
                                       "- SpaceX is connected to NASA through 8 different missions\n" +
                                       "- Identified 12 key research institutions collaborating on space science\n" +
                                       "- Found 5 major funding sources connecting multiple projects\n" +
                                       "- Detected temporal relationships between mission announcements and funding\n\n" +
                                       "Would you like me to visualize these relationships or explore a specific connection?";
                        
                        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, result));
                        updateStatus("Relationship mapping completed");
                    });
                    
                } catch (InterruptedException e) {
                    logger.error("Analysis processing interrupted", e);
                    Platform.runLater(() -> updateStatus("Analysis failed"));
                }
            }).start();
        } else {
            // For other methods, just update status
            updateStatus("Analyzed " + selectedDatasets.size() + " datasets using " + analysisType);
        }
    }
    
    /**
     * Adds a placeholder conversation to demonstrate AI functionality.
     */
    private void addPlaceholderConversation() {
        // Add a welcome message
        addMessageToUI(new AIMessage(AIMessage.Role.SYSTEM, 
            "Welcome to the Analysis AI Assistant. I can help you find patterns, relationships, " +
            "and insights in your archived space data."
        ));
        
        // Add a sample conversation
        addMessageToUI(new AIMessage(AIMessage.Role.USER, 
            "Can you analyze the trends in space missions over the last decade?"
        ));
        
        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, 
            "I've analyzed space mission data from the past decade and found these key trends:\n\n" +
            "1. Commercial space activity has increased by 320% since 2012\n" +
            "2. Small satellite launches grew at 27% CAGR (compound annual growth rate)\n" +
            "3. Mars missions peaked in 2018-2020, while lunar missions are now accelerating\n" +
            "4. International cooperation appears in 68% of major scientific missions\n" +
            "5. Reusable launch systems became dominant after 2017\n\n" +
            "Would you like me to explore any of these trends in more detail?"
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