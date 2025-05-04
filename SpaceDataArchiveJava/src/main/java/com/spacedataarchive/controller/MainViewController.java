package com.spacedataarchive.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import com.spacedataarchive.model.SystemMetric;
import com.spacedataarchive.service.LogService;
import com.spacedataarchive.theme.AtlantaThemeManager;
import com.spacedataarchive.monitoring.OperationsDashboardPane;
import com.spacedataarchive.controller.OperationsDashboardController;

/**
 * Controller for the main view of the application.
 */
public class MainViewController {
    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);
    
    // Tab pane
    @FXML private TabPane mainTabPane;
    
    // Status indicators
    @FXML private Label statusLabel;
    @FXML private Label statusBarLabel;
    @FXML private ProgressBar statusProgressBar;
    
    // Logo
    @FXML private javafx.scene.image.ImageView logoImageView;
    
    // Logs tab controls
    @FXML private TextArea logsTextArea;
    @FXML private ComboBox<String> logLevelCombo;
    @FXML private TextArea systemInfoArea;
    @FXML private TableView<SystemMetric> metricsTable;
    @FXML private TableColumn<SystemMetric, String> metricNameColumn;
    @FXML private TableColumn<SystemMetric, String> metricValueColumn;
    
    // Operations Dashboard
    @FXML private StackPane operationsDashboardContainer;
    @FXML private OperationsDashboardController operationsDashboardController;
    private OperationsDashboardPane operationsDashboardPane;
    
    // Root container
    @FXML private BorderPane mainBorderPane;
    
    // Theme toggle button
    @FXML private ToggleButton themeToggleButton;
    
    // Services
    private LogService logService;
    
    // AI Chat controller
    private AIChatController aiChatController;
    private Button aiChatButton;
    
    /**
     * Initializes the controller.
     * This method is automatically called after the FXML file has been loaded.
     */
    @FXML
    private void initialize() {
        logger.info("Initializing MainViewController");
        
        // Initialize services
        logService = new LogService();
        
        // Initialize UI components
        initializeLogsTab();
        
        // Setup log redirection to the logs text area
        setupLogRedirection();
        
        // Show system information
        updateSystemInfo();
        
        // Initialize theme toggle
        initializeThemeToggle();
        
        // Add Operations Dashboard if not already added
        if (operationsDashboardController != null) {
            initializeOperationsDashboard();
        }
        
        // Monitor tab selections
        mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab != null && newTab.getText().equals("Operations Dashboard")) {
                if (operationsDashboardPane != null) {
                    operationsDashboardPane.startMonitoring();
                }
            } else if (oldTab != null && oldTab.getText().equals("Operations Dashboard")) {
                if (operationsDashboardPane != null) {
                    operationsDashboardPane.stopMonitoring();
                }
            }
        });
        
        try {
            addAIAssistant();
        } catch (Exception e) {
            logger.error("Failed to add AI Assistant button", e);
        }
        
        // Initialize the logo based on current theme
        initializeLogo();
        
        // Log successful initialization
        logger.info("MainViewController initialized successfully");
        updateStatus("Application ready");
    }
    
    /**
     * Initializes the theme toggle button
     */
    private void initializeThemeToggle() {
        if (themeToggleButton != null) {
            AtlantaThemeManager themeManager = AtlantaThemeManager.getInstance();
            boolean isDarkTheme = themeManager.isDarkTheme();
            
            // Set initial state based on current theme
            themeToggleButton.setSelected(isDarkTheme);
            updateThemeToggleText();
            
            // Add listener for toggle changes
            themeToggleButton.selectedProperty().addListener((observable, oldValue, newValue) -> {
                // Toggle between light and dark versions of the current theme family
                logger.info("Theme toggle switched to: {}", newValue ? "Dark" : "Light");
                themeManager.toggleLightDark();
                updateThemeToggleText();
            });
            
            logger.info("Theme toggle initialized with current theme: {}", themeManager.getThemeName());
        } else {
            logger.warn("Theme toggle button not found in FXML");
        }
    }
    
    /**
     * Updates the text on the theme toggle button
     */
    private void updateThemeToggleText() {
        if (themeToggleButton.isSelected()) {
            themeToggleButton.setText("Dark");
        } else {
            themeToggleButton.setText("Light");
        }
    }
    
    /**
     * Initializes the Logs tab.
     */
    private void initializeLogsTab() {
        // Setup log level combo
        logLevelCombo.getItems().addAll("DEBUG", "INFO", "WARN", "ERROR");
        logLevelCombo.getSelectionModel().select("INFO");
        logLevelCombo.setOnAction(event -> {
            String selectedLevel = logLevelCombo.getSelectionModel().getSelectedItem();
            logService.setLogLevel(selectedLevel);
            logger.info("Log level set to: {}", selectedLevel);
        });
        
        // Setup metrics table
        metricNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        metricValueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        
        // Add some initial system metrics
        updateMetrics();
    }
    
    /**
     * Sets up log redirection to the log text area.
     */
    private void setupLogRedirection() {
        // Register a log appender that will update our text area
        logService.setLogConsumer(logMessage -> {
            if (logsTextArea != null) {
                Platform.runLater(() -> {
                    logsTextArea.appendText(logMessage + "\n");
                });
            }
        });
    }
    
    /**
     * Updates the system information displayed.
     */
    private void updateSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("JavaFX Version: ").append(System.getProperty("javafx.version")).append("\n");
        info.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append("\n");
        info.append("User: ").append(System.getProperty("user.name")).append("\n");
        info.append("Available Processors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        info.append("Max Memory: ").append(Runtime.getRuntime().maxMemory() / 1048576).append(" MB\n");
        
        systemInfoArea.setText(info.toString());
    }
    
    /**
     * Updates the performance metrics.
     */
    private void updateMetrics() {
        // This would be replaced with actual metrics in a real application
        metricsTable.getItems().clear();
        metricsTable.getItems().add(new SystemMetric("CPU Usage", "2%"));
        metricsTable.getItems().add(new SystemMetric("Memory Usage", "124 MB"));
        metricsTable.getItems().add(new SystemMetric("Disk I/O", "0.5 MB/s"));
        metricsTable.getItems().add(new SystemMetric("Network I/O", "0.1 MB/s"));
    }
    
    /**
     * Updates the status message in the status bar and label.
     * 
     * @param message The message to display
     */
    private void updateStatus(String message) {
        statusBarLabel.setText(message);
        statusLabel.setText(message);
        logger.debug("Status updated: {}", message);
    }
    
    /**
     * Handles the Clear Logs button click.
     */
    @FXML
    private void handleClearLogs() {
        logsTextArea.clear();
        logger.info("Logs cleared");
    }
    
    /**
     * Handles the Export Logs button click.
     */
    @FXML
    private void handleExportLogs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Logs");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Log Files", "*.log"));
        fileChooser.setInitialFileName("space-data-archive-logs.log");
        
        File file = fileChooser.showSaveDialog(logsTextArea.getScene().getWindow());
        if (file != null) {
            try {
                logService.exportLogs(file, logsTextArea.getText());
                logger.info("Logs exported to: {}", file.getAbsolutePath());
                updateStatus("Logs exported successfully");
            } catch (Exception e) {
                logger.error("Failed to export logs", e);
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to export logs: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles the Preferences menu item.
     */
    @FXML
    private void handlePreferences() {
        logger.info("Preferences menu item clicked");
        showAlert(Alert.AlertType.INFORMATION, "Preferences", "Preferences dialog will be implemented in Phase 2.");
    }
    
    /**
     * Handles the About menu item.
     */
    @FXML
    private void handleAbout() {
        logger.info("About menu item clicked");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Space Data Archive System");
        alert.setContentText("Version 1.0\n\nA Java application for archiving and analyzing space-related data.");
        alert.showAndWait();
    }
    
    /**
     * Handles the Exit menu item.
     */
    @FXML
    private void handleExit() {
        logger.info("Exit menu item clicked");
        Platform.exit();
    }
    
    /**
     * Shows an alert dialog.
     * 
     * @param type The alert type
     * @param title The alert title
     * @param content The alert content
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Adds the AI assistant button to the UI.
     */
    private void addAIAssistant() {
        try {
            // Create a container for the main content
            StackPane rootContainer = new StackPane();
            
            // Add main content to the stack pane
            BorderPane contentPane = new BorderPane();
            contentPane.setTop(mainBorderPane.getTop());
            contentPane.setCenter(mainTabPane);
            contentPane.setBottom(mainBorderPane.getBottom());
            rootContainer.getChildren().add(contentPane);
            
            // Create AI Assistant button
            aiChatButton = new Button();
            aiChatButton.getStyleClass().add("ai-chat-trigger");
            
            // Add icon
            try {
                ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream("/images/ai-icon.svg")));
                iconView.setFitHeight(28);
                iconView.setFitWidth(28);
                iconView.setPreserveRatio(true);
                aiChatButton.setGraphic(iconView);
            } catch (Exception e) {
                logger.warn("Failed to load AI icon, using text instead", e);
                aiChatButton.setText("AI");
            }
            
            // Add the button with positioning
            StackPane.setAlignment(aiChatButton, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(aiChatButton, new javafx.geometry.Insets(0, 20, 20, 0));
            rootContainer.getChildren().add(aiChatButton);
            
            // Set the root container as the center of the main border pane
            mainBorderPane.setCenter(rootContainer);
            
            // Create AI chat controller
            aiChatController = new AIChatController();
            
            // Set up the button action
            aiChatButton.setOnAction(event -> aiChatController.toggle(aiChatButton));
            
            // Add the stylesheet
            mainBorderPane.getScene().getStylesheets().add(getClass().getResource("/css/ai-chat.css").toExternalForm());
            
            logger.info("Added AI Assistant button");
        } catch (Exception e) {
            logger.error("Failed to add AI Assistant button", e);
        }
    }
    
    /**
     * Initializes the logo based on the current theme.
     */
    private void initializeLogo() {
        if (logoImageView != null) {
            AtlantaThemeManager themeManager = AtlantaThemeManager.getInstance();
            boolean isDarkTheme = themeManager.isDarkTheme();
            
            // Loading a themed logo could be done here
            logger.info("Initializing logo for {} theme", isDarkTheme ? "dark" : "light");
        } else {
            logger.warn("Logo image view not found in FXML");
        }
    }
    
    /**
     * Initializes the Operations Dashboard tab
     */
    private void initializeOperationsDashboard() {
        try {
            // Create operations dashboard if it's not already in the FXML
            if (operationsDashboardPane == null) {
                operationsDashboardPane = new OperationsDashboardPane();
                if (operationsDashboardContainer != null) {
                    operationsDashboardContainer.getChildren().add(operationsDashboardPane);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Operations Dashboard", e);
        }
    }
} 