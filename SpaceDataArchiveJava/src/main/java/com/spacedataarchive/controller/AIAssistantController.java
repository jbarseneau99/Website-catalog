package com.spacedataarchive.controller;

import com.spacedataarchive.model.AIConversation;
import com.spacedataarchive.model.AIMessage;
import com.spacedataarchive.service.ai.AnthropicAIService;
import com.spacedataarchive.service.ai.ConversationService;
import com.spacedataarchive.service.ai.AIServiceManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Controller for the AI Assistant tab.
 */
public class AIAssistantController {
    private static final Logger logger = LoggerFactory.getLogger(AIAssistantController.class);
    
    @FXML private ListView<AIConversation> conversationListView;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private TextField apiKeyField;
    @FXML private Button saveApiKeyButton;
    @FXML private Button newChatButton;
    @FXML private Label statusLabel;
    
    private AnthropicAIService aiService;
    private ConversationService conversationService;
    private ObservableList<AIConversation> conversationsObservable;
    private String currentConversationId;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing AIAssistantController");
        
        // Initialize the AI service
        aiService = AIServiceManager.getInstance().getAiService();
        conversationService = new ConversationService(aiService);
        
        // Setup conversation list
        conversationsObservable = FXCollections.observableArrayList(conversationService.getAllConversations());
        conversationListView.setItems(conversationsObservable);
        conversationListView.setCellFactory(param -> new ConversationListCell());
        conversationListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentConversationId = newVal.getId();
                refreshChatView();
            }
        });
        
        // Create initial conversation if none exist
        if (conversationsObservable.isEmpty()) {
            AIConversation conversation = conversationService.createConversation(
                "AI Assistant Chat", "ai_assistant_tab");
            currentConversationId = conversation.getId();
            refreshConversationList();
            
            // Select the new conversation
            for (int i = 0; i < conversationsObservable.size(); i++) {
                if (conversationsObservable.get(i).getId().equals(currentConversationId)) {
                    conversationListView.getSelectionModel().select(i);
                    break;
                }
            }
        } else {
            // Select the first conversation
            currentConversationId = conversationsObservable.get(0).getId();
            conversationListView.getSelectionModel().select(0);
        }
        
        // Setup model dropdown
        modelComboBox.setItems(FXCollections.observableArrayList(aiService.getAvailableModels()));
        modelComboBox.getSelectionModel().select(aiService.getModel());
        modelComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                aiService.updateModel(newVal);
                updateStatus("Model updated to " + newVal);
            }
        });
        
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
        
        // Setup API key save button
        saveApiKeyButton.setOnAction(event -> {
            String apiKey = apiKeyField.getText();
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                boolean success = aiService.updateApiKey(apiKey);
                if (success) {
                    updateStatus("API key saved successfully");
                    apiKeyField.setText("");
                } else {
                    updateStatus("Failed to save API key");
                }
            } else {
                updateStatus("API key cannot be empty");
            }
        });
        
        // Setup new chat button
        setupNewChatButton();
        
        // Initial status update
        updateStatus("AI Assistant ready" + (aiService.isInitialized() ? "" : " (API key not configured)"));
        
        // Initial chat view refresh
        refreshChatView();
    }
    
    /**
     * Sends a message to the AI.
     */
    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty() || currentConversationId == null) {
            return;
        }
        
        // Clear the input field
        messageInput.clear();
        
        // Disable the send button while waiting for a response
        sendButton.setDisable(true);
        updateStatus("Waiting for AI response...");
        
        // Create and add user message
        AIMessage userMessage = new AIMessage(AIMessage.Role.USER, message);
        conversationService.addMessage(currentConversationId, userMessage);
        
        // Add user message to UI
        addMessageToUI(userMessage);
        
        // Send the message in a background thread
        new Thread(() -> {
            try {
                // Send the message to the AI service
                AIMessage response = conversationService.sendMessage(currentConversationId, message);
                
                Platform.runLater(() -> {
                    // Add AI response to UI
                    addMessageToUI(response);
                    
                    // Re-enable the send button
                    sendButton.setDisable(false);
                    updateStatus("Ready");
                    
                    // Refresh the conversation list to show updated titles
                    refreshConversationList();
                });
            } catch (Exception e) {
                logger.error("Error sending message to AI", e);
                
                Platform.runLater(() -> {
                    // Add error message to UI
                    addMessageToUI(new AIMessage(AIMessage.Role.SYSTEM, "Error communicating with AI: " + e.getMessage()));
                    
                    // Re-enable the send button
                    sendButton.setDisable(false);
                    updateStatus("Error communicating with AI");
                });
            }
        }).start();
    }
    
    /**
     * Adds a message to the chat UI.
     * 
     * @param message The message to add
     */
    private void addMessageToUI(AIMessage message) {
        // Create a new message bubble
        VBox messageBox = new VBox();
        messageBox.setSpacing(5);
        messageBox.setPadding(new Insets(10));
        messageBox.setMaxWidth(chatContainer.getWidth() - 20);
        
        // Add sender info
        Label senderLabel = new Label(message.isFromUser() ? "You" : message.isFromAssistant() ? "Claude" : "System");
        senderLabel.getStyleClass().add("message-sender");
        
        // Add timestamp
        Label timestampLabel = new Label(message.getFormattedTimestamp());
        timestampLabel.getStyleClass().add("message-timestamp");
        
        // Create header with sender and timestamp
        HBox headerBox = new HBox(senderLabel, timestampLabel);
        headerBox.setSpacing(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        // Add the message content
        TextFlow contentFlow = new TextFlow();
        Text contentText = new Text(message.getContent());
        contentText.wrappingWidthProperty().bind(chatContainer.widthProperty().subtract(40));
        contentFlow.getChildren().add(contentText);
        
        // Add all components to the message box
        messageBox.getChildren().addAll(headerBox, contentFlow);
        
        // Style based on sender
        messageBox.getStyleClass().add(message.isFromUser() ? "user-message" : 
                                     message.isFromAssistant() ? "assistant-message" : "system-message");
        
        // Add to the chat container
        chatContainer.getChildren().add(messageBox);
        
        // Scroll to the bottom
        chatScrollPane.setVvalue(1.0);
    }
    
    /**
     * Refreshes the chat view with messages from the current conversation.
     */
    private void refreshChatView() {
        chatContainer.getChildren().clear();
        
        if (currentConversationId != null) {
            AIConversation conversation = conversationService.getConversation(currentConversationId);
            if (conversation != null) {
                for (AIMessage message : conversation.getMessages()) {
                    addMessageToUI(message);
                }
            }
        }
    }
    
    /**
     * Refreshes the conversation list.
     */
    private void refreshConversationList() {
        conversationsObservable.setAll(conversationService.getAllConversations());
    }
    
    /**
     * Updates the status label.
     * 
     * @param status The status message
     */
    private void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Opens the Anthropic website in the default browser.
     */
    @FXML
    private void openAnthropicWebsite() {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://anthropic.com"));
            logger.info("Opened Anthropic website");
        } catch (Exception e) {
            logger.error("Failed to open Anthropic website", e);
            updateStatus("Failed to open website");
        }
    }
    
    /**
     * Custom cell factory for rendering conversations in the list view.
     */
    private class ConversationListCell extends ListCell<AIConversation> {
        private final ContextMenu contextMenu = new ContextMenu();
        
        public ConversationListCell() {
            // Create context menu for conversation items
            createContextMenu();
        }
        
        @Override
        protected void updateItem(AIConversation conversation, boolean empty) {
            super.updateItem(conversation, empty);
            
            if (empty || conversation == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
            } else {
                setText(conversation.getTitle() + " (" + conversation.getFormattedCreatedAt() + ")");
                setContextMenu(contextMenu);
            }
        }
        
        /**
         * Creates the context menu for conversation items.
         */
        private void createContextMenu() {
            MenuItem renameItem = new MenuItem("Rename");
            renameItem.setOnAction(event -> {
                AIConversation conversation = getItem();
                if (conversation != null) {
                    TextInputDialog dialog = new TextInputDialog(conversation.getTitle());
                    dialog.setTitle("Rename Conversation");
                    dialog.setHeaderText("Enter a new title for the conversation");
                    dialog.setContentText("Title:");
                    
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(title -> {
                        conversationService.updateTitle(conversation.getId(), title);
                        refreshConversationList();
                    });
                }
            });
            
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(event -> {
                AIConversation conversation = getItem();
                if (conversation != null) {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Delete Conversation");
                    alert.setHeaderText("Delete " + conversation.getTitle());
                    alert.setContentText("Are you sure you want to delete this conversation?");
                    
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        conversationService.deleteConversation(conversation.getId());
                        
                        // If we deleted the current conversation, create a new one
                        if (conversation.getId().equals(currentConversationId)) {
                            AIConversation newConversation = conversationService.createConversation(
                                "AI Assistant Chat", "ai_assistant_tab");
                            currentConversationId = newConversation.getId();
                        }
                        
                        refreshConversationList();
                        
                        // Select a conversation (either the first one or the current one)
                        if (!conversationsObservable.isEmpty()) {
                            for (int i = 0; i < conversationsObservable.size(); i++) {
                                if (conversationsObservable.get(i).getId().equals(currentConversationId)) {
                                    conversationListView.getSelectionModel().select(i);
                                    break;
                                }
                            }
                        }
                        
                        refreshChatView();
                    }
                }
            });
            
            contextMenu.getItems().addAll(renameItem, deleteItem);
        }
    }
    
    /**
     * Setup new chat button
     */
    @FXML
    private void setupNewChatButton() {
        // Setup new chat button
        newChatButton.setOnAction(event -> {
            AIConversation newConversation = conversationService.createConversation(
                "New Conversation", "ai_assistant_tab");
            currentConversationId = newConversation.getId();
            refreshConversationList();
            
            // Select the new conversation
            for (int i = 0; i < conversationsObservable.size(); i++) {
                if (conversationsObservable.get(i).getId().equals(currentConversationId)) {
                    conversationListView.getSelectionModel().select(i);
                    break;
                }
            }
            
            refreshChatView();
        });
    }
} 