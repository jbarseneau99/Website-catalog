package com.spacedataarchive.controller;

import com.spacedataarchive.model.AIMessage;
import com.spacedataarchive.service.ai.AIMemoryService;
import com.spacedataarchive.service.ai.AIMemoryService.AIFunctionMetadata;
import com.spacedataarchive.service.ai.AIMemoryService.AIFunctionType;
import com.spacedataarchive.service.ai.AIServiceManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller for the AI Registry tab.
 */
public class AIRegistryController {
    private static final Logger logger = LoggerFactory.getLogger(AIRegistryController.class);
    
    @FXML private TableView<AIFunctionMetadata> functionsTableView;
    @FXML private TableColumn<AIFunctionMetadata, String> idColumn;
    @FXML private TableColumn<AIFunctionMetadata, String> nameColumn;
    @FXML private TableColumn<AIFunctionMetadata, String> typeColumn;
    @FXML private TableColumn<AIFunctionMetadata, Integer> messagesColumn;
    @FXML private TableColumn<AIFunctionMetadata, Void> actionsColumn;
    
    @FXML private TextField functionIdField;
    @FXML private TextField functionNameField;
    @FXML private ComboBox<AIFunctionType> functionTypeComboBox;
    @FXML private TextArea descriptionTextArea;
    
    @FXML private TableView<AIMessage> messagesTableView;
    @FXML private TableColumn<AIMessage, String> roleColumn;
    @FXML private TableColumn<AIMessage, String> contentColumn;
    @FXML private TableColumn<AIMessage, String> timestampColumn;
    
    @FXML private Button refreshButton;
    @FXML private Button addFunctionButton;
    @FXML private Button saveButton;
    @FXML private Button clearMessagesButton;
    
    @FXML private ComboBox<AIFunctionType> filterTypeComboBox;
    @FXML private TextField searchField;
    
    @FXML private Label statusLabel;
    @FXML private Label memoryStatsLabel;
    
    private AIMemoryService memoryService;
    private Map<String, Integer> messageCountMap;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing AIRegistryController");
        
        // Initialize memory service
        memoryService = AIServiceManager.getInstance().getMemoryService();
        
        // Setup table columns
        setupTableColumns();
        
        // Setup function type combo box
        functionTypeComboBox.setItems(FXCollections.observableArrayList(AIFunctionType.values()));
        filterTypeComboBox.setItems(FXCollections.observableArrayList(AIFunctionType.values()));
        filterTypeComboBox.getItems().add(0, null);
        
        // Load functions
        refreshFunctions();
        
        // Setup event handlers
        setupEventHandlers();
        
        updateStatus("AI Registry ready");
    }
    
    /**
     * Sets up the table columns.
     */
    private void setupTableColumns() {
        // Functions table
        idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().toString()));
        
        messagesColumn.setCellValueFactory(cellData -> {
            String id = cellData.getValue().getId();
            Integer count = messageCountMap.getOrDefault(id, 0);
            return new SimpleIntegerProperty(count).asObject();
        });
        
        // Setup action column
        actionsColumn.setCellFactory(createActionColumnCellFactory());
        
        // Messages table
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().toString()));
        contentColumn.setCellValueFactory(cellData -> {
            String content = cellData.getValue().getContent();
            if (content.length() > 50) {
                content = content.substring(0, 47) + "...";
            }
            return new SimpleStringProperty(content);
        });
        timestampColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp().toString()));
    }
    
    /**
     * Creates a cell factory for the action column.
     */
    private Callback<TableColumn<AIFunctionMetadata, Void>, TableCell<AIFunctionMetadata, Void>> createActionColumnCellFactory() {
        return param -> new TableCell<>() {
            private final Button viewButton = new Button("View");
            private final Button deleteButton = new Button("Delete");
            
            {
                viewButton.setOnAction(event -> {
                    AIFunctionMetadata function = getTableView().getItems().get(getIndex());
                    viewFunction(function);
                });
                
                deleteButton.setOnAction(event -> {
                    AIFunctionMetadata function = getTableView().getItems().get(getIndex());
                    deleteFunction(function);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        };
    }
    
    /**
     * Sets up event handlers.
     */
    private void setupEventHandlers() {
        refreshButton.setOnAction(event -> refreshFunctions());
        
        addFunctionButton.setOnAction(event -> showAddFunctionDialog());
        
        saveButton.setOnAction(event -> saveFunction());
        
        clearMessagesButton.setOnAction(event -> clearMessages());
        
        filterTypeComboBox.setOnAction(event -> applyFilters());
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    /**
     * Refreshes the function list.
     */
    private void refreshFunctions() {
        List<AIFunctionMetadata> functions = memoryService.getAllFunctions();
        messageCountMap = memoryService.getMemoryUsage();
        
        functionsTableView.setItems(FXCollections.observableArrayList(functions));
        
        int totalMessages = messageCountMap.values().stream().mapToInt(Integer::intValue).sum();
        memoryStatsLabel.setText(functions.size() + " functions, " + totalMessages + " messages");
        
        updateStatus("Functions refreshed");
    }
    
    /**
     * Shows the add function dialog.
     */
    private void showAddFunctionDialog() {
        // Clear form fields
        functionIdField.clear();
        functionNameField.clear();
        descriptionTextArea.clear();
        functionTypeComboBox.setValue(AIFunctionType.FUNCTION);
        
        // Clear message table
        messagesTableView.getItems().clear();
        
        // Make ID field editable for new function
        functionIdField.setEditable(true);
        
        updateStatus("Enter new function details");
    }
    
    /**
     * Views a function.
     * 
     * @param function The function to view
     */
    private void viewFunction(AIFunctionMetadata function) {
        functionIdField.setText(function.getId());
        functionNameField.setText(function.getName());
        descriptionTextArea.setText(function.getDescription());
        functionTypeComboBox.setValue(function.getType());
        
        // Make ID field non-editable for existing function
        functionIdField.setEditable(false);
        
        // Load messages
        List<AIMessage> messages = memoryService.getMessages(function.getId());
        messagesTableView.setItems(FXCollections.observableArrayList(messages));
        
        updateStatus("Viewing function: " + function.getName());
    }
    
    /**
     * Saves a function.
     */
    private void saveFunction() {
        String id = functionIdField.getText();
        String name = functionNameField.getText();
        String description = descriptionTextArea.getText();
        AIFunctionType type = functionTypeComboBox.getValue();
        
        if (id.isEmpty() || name.isEmpty() || type == null) {
            updateStatus("Please fill in all required fields");
            return;
        }
        
        boolean success;
        if (functionIdField.isEditable()) {
            // New function
            success = memoryService.registerFunction(id, name, description, type);
        } else {
            // Update existing function
            success = memoryService.updateFunction(id, name, description);
        }
        
        if (success) {
            refreshFunctions();
            updateStatus("Function saved: " + name);
        } else {
            updateStatus("Failed to save function");
        }
    }
    
    /**
     * Deletes a function.
     * 
     * @param function The function to delete
     */
    private void deleteFunction(AIFunctionMetadata function) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Function");
        alert.setHeaderText("Delete Function: " + function.getName());
        alert.setContentText("Are you sure you want to delete this function? All memory segments will be permanently lost.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = memoryService.deleteFunction(function.getId());
                
                if (success) {
                    refreshFunctions();
                    updateStatus("Function deleted: " + function.getName());
                } else {
                    updateStatus("Failed to delete function");
                }
            }
        });
    }
    
    /**
     * Clears all messages for the selected function.
     */
    private void clearMessages() {
        String id = functionIdField.getText();
        
        if (id.isEmpty()) {
            updateStatus("No function selected");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Messages");
        alert.setHeaderText("Clear All Messages");
        alert.setContentText("Are you sure you want to clear all messages for this function?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = memoryService.clearMessages(id);
                
                if (success) {
                    messagesTableView.getItems().clear();
                    refreshFunctions();
                    updateStatus("Messages cleared for function: " + id);
                } else {
                    updateStatus("Failed to clear messages");
                }
            }
        });
    }
    
    /**
     * Applies filters to the function list.
     */
    private void applyFilters() {
        List<AIFunctionMetadata> functions = memoryService.getAllFunctions();
        
        // Apply type filter
        AIFunctionType typeFilter = filterTypeComboBox.getValue();
        if (typeFilter != null) {
            functions.removeIf(function -> function.getType() != typeFilter);
        }
        
        // Apply search filter
        String searchText = searchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            functions.removeIf(function -> 
                !function.getId().toLowerCase().contains(searchText) && 
                !function.getName().toLowerCase().contains(searchText) &&
                !function.getDescription().toLowerCase().contains(searchText));
        }
        
        functionsTableView.setItems(FXCollections.observableArrayList(functions));
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