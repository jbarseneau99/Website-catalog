package com.spacedataarchive.controller;

import com.spacedataarchive.model.AIMessage;
import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.EnhancedValidationResult;
import com.spacedataarchive.model.ValidatedUrlCollection;
import com.spacedataarchive.service.ServiceFactory;
import com.spacedataarchive.service.SiteMapService;
import com.spacedataarchive.service.URLValidationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the URL Validation tab.
 * Handles validation of URLs from site maps.
 */
public class URLValidationController {
    private static final Logger logger = LoggerFactory.getLogger(URLValidationController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatContainer;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private Label statusLabel;
    @FXML private Button validateButton;
    @FXML private Button stopButton;
    @FXML private ComboBox<String> validationMethodComboBox;
    @FXML private ComboBox<SiteMapProject> siteMapComboBox;
    @FXML private Label urlCountLabel;
    @FXML private Label queueTotalLabel;
    @FXML private Label urlSourceLabel;
    @FXML private TableView<URLValidationResult> resultsTableView;
    @FXML private TableColumn<URLValidationResult, String> urlColumn;
    @FXML private TableColumn<URLValidationResult, String> statusColumn;
    @FXML private TableColumn<URLValidationResult, String> messageColumn;
    @FXML private ProgressBar validationProgressBar;
    @FXML private Label progressLabel;
    @FXML private Pagination resultsPagination;
    @FXML private Label performanceLabel;
    @FXML private TableColumn<URLValidationResult, String> titleColumn;
    @FXML private TableColumn<URLValidationResult, String> assetTypeColumn;
    
    private URLValidationService validationService;
    private SiteMapService siteMapService;
    private String currentValidationId;
    private CompletableFuture<ValidatedUrlCollection> currentValidationFuture;
    private final AtomicBoolean validationActive = new AtomicBoolean(false);
    private Thread progressUpdateThread;
    
    // Observable list for site map projects
    private final ObservableList<SiteMapProject> siteMapProjects = FXCollections.observableArrayList();
    // Observable list for validation results
    private final ObservableList<URLValidationResult> validationResults = FXCollections.observableArrayList();
    
    private static final int ROWS_PER_PAGE = 50;
    private final ObservableList<URLValidationResult> allValidationResults = FXCollections.observableArrayList();
    
    // Add a field to store loaded URLs
    private List<String> loadedUrls = new ArrayList<>();
    
    // Field to store site map ID for deferred loading
    private String loadedDatasetId;
    
    // Field for reload button
    private Button reloadButton;
    
    // Add field for status container
    @FXML private HBox statusContainer;
    
    /**
     * SiteMap project wrapper for ComboBox display
     */
    public static class SiteMapProject {
        private final String id;
        private final String name;
        
        public SiteMapProject(String id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing URLValidationController");
        
        // Get services from the factory
        validationService = ServiceFactory.getInstance().getUrlValidationService();
        siteMapService = ServiceFactory.getInstance().getSiteMapService();
        
        // Initialize UI components
        initializeUI();
        
        // Load site map projects
        loadSiteMapProjects();
        
        // Add placeholder conversation to demonstrate AI functionality
        addPlaceholderConversation();
        
        updateStatus("URL Validation tab ready");
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        // Configure message input
        messageInput.setWrapText(true);
        messageInput.setPromptText("Ask about URL validation...");
        
        // Configure send button
        sendButton.setOnAction(e -> sendMessage());
        
        // Configure validation method combo box
        validationMethodComboBox.getItems().addAll(
            "Basic URL Validation",
            "Enhanced Validation",
            "AI-assisted Validation"
        );
        validationMethodComboBox.getSelectionModel().selectFirst();
        
        // Configure site map combo box
        siteMapComboBox.setItems(siteMapProjects);
        siteMapComboBox.setPromptText("Select a site map");
        siteMapComboBox.setCellFactory(lv -> new ListCell<SiteMapProject>() {
            @Override
            protected void updateItem(SiteMapProject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        siteMapComboBox.setButtonCell(new ListCell<SiteMapProject>() {
            @Override
            protected void updateItem(SiteMapProject item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });
        
        siteMapComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                logger.info("Site map selected: {} (ID: {})", newValue.getName(), newValue.getId());
                loadUrlsFromSiteMap(newValue.getId());
            }
        });
        
        // Initialize stats labels
        urlCountLabel.setText("0");
        queueTotalLabel.setText("0");
        urlSourceLabel.setText("No site map selected");
        
        // Configure validate button
        validateButton.setOnAction(e -> startValidation());
        
        // Configure stop button
        stopButton.setOnAction(e -> stopValidation());
        stopButton.setDisable(true);
        
        // Initialize the results table if needed
        initializeResultsTable();
        
        // Configure pagination if available in FXML
        if (resultsPagination != null) {
            resultsPagination.setPageCount(1);
            resultsPagination.setCurrentPageIndex(0);
            resultsPagination.setPageFactory(this::createPage);
        }
        
        // Configure status display
        validationProgressBar.setProgress(0);
        progressLabel.setText("0%");
        
        // Initialize performance label if available
        if (performanceLabel != null) {
            performanceLabel.setText("0 URLs/sec");
        }
    }
    
    /**
     * Initializes the results table and its columns.
     */
    private void initializeResultsTable() {
        // Set up table data
        resultsTableView.setItems(validationResults);
        
        // Initialize columns if they don't exist
        if (urlColumn == null) {
            urlColumn = new TableColumn<>("URL");
            resultsTableView.getColumns().add(urlColumn);
        }
        
        if (statusColumn == null) {
            statusColumn = new TableColumn<>("Status");
            resultsTableView.getColumns().add(statusColumn);
        }
        
        if (messageColumn == null) {
            messageColumn = new TableColumn<>("Message");
            resultsTableView.getColumns().add(messageColumn);
        }
        
        if (titleColumn == null) {
            titleColumn = new TableColumn<>("Title");
            resultsTableView.getColumns().add(titleColumn);
        }
        
        if (assetTypeColumn == null) {
            assetTypeColumn = new TableColumn<>("Content Type");
            resultsTableView.getColumns().add(assetTypeColumn);
        }
        
        // Set up cell value factories
        urlColumn.setCellValueFactory(cellData -> cellData.getValue().urlProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        messageColumn.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        assetTypeColumn.setCellValueFactory(cellData -> cellData.getValue().assetTypeProperty());
        
        // Set column widths if needed
        urlColumn.setPrefWidth(300);
        statusColumn.setPrefWidth(100);
        messageColumn.setPrefWidth(200);
        titleColumn.setPrefWidth(200);
        assetTypeColumn.setPrefWidth(150);
        
        // Configure URL column to use hyperlinks
        setupUrlColumnWithLinks();
        
        // Configure status column with colored cells
        setupStatusColumnWithColors();
        
        // Configure asset type column with appropriate styling
        setupAssetTypeColumnWithIcons();
    }
    
    /**
     * Sets up the URL column to use hyperlinks for valid URLs.
     */
    private void setupUrlColumnWithLinks() {
        // Set up URL column with clickable links for valid assets
        urlColumn.setCellFactory(col -> new TableCell<URLValidationResult, String>() {
            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                
                if (empty || url == null || url.isEmpty()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Get complete row data
                    URLValidationResult rowData = getTableView().getItems().get(getIndex());
                    
                    // Only create hyperlinks for valid or warning status items
                    if (rowData != null && ("Valid".equals(rowData.getStatus()) || "Warning".equals(rowData.getStatus()))) {
                        Hyperlink hyperlink = new Hyperlink(url);
                        hyperlink.setOnAction(e -> {
                            try {
                                Desktop.getDesktop().browse(new URI(url));
                            } catch (Exception ex) {
                                logger.error("Error opening URL: {}", url, ex);
                                
                                // Show error dialog
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Error Opening URL");
                                alert.setHeaderText("Could not open URL");
                                alert.setContentText("Error: " + ex.getMessage());
                                alert.show();
                            }
                        });
                        setGraphic(hyperlink);
                        setText(null);
                    } else {
                        setText(url);
                        setGraphic(null);
                    }
                }
            }
        });
    }
    
    /**
     * Sets up the status column with colored cells based on status.
     */
    private void setupStatusColumnWithColors() {
        statusColumn.setCellFactory(col -> new TableCell<URLValidationResult, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                
                if (empty || status == null || status.isEmpty()) {
                    setText(null);
                    setBackground(Background.EMPTY);
                } else {
                    setText(status);
                    
                    // Set background color based on status
                    BackgroundFill fill;
                    switch (status) {
                        case "Valid":
                            fill = new BackgroundFill(Color.rgb(200, 255, 200), new CornerRadii(3), Insets.EMPTY);
                            break;
                        case "Warning":
                            fill = new BackgroundFill(Color.rgb(255, 255, 200), new CornerRadii(3), Insets.EMPTY);
                            break;
                        case "Error":
                            fill = new BackgroundFill(Color.rgb(255, 200, 200), new CornerRadii(3), Insets.EMPTY);
                            break;
                        case "In Progress":
                            fill = new BackgroundFill(Color.rgb(220, 220, 255), new CornerRadii(3), Insets.EMPTY);
                            break;
                        default:
                            fill = new BackgroundFill(Color.TRANSPARENT, new CornerRadii(3), Insets.EMPTY);
                    }
                    
                    setBackground(new Background(fill));
                }
            }
        });
    }
    
    /**
     * Sets up the asset type column with styling based on asset type.
     */
    private void setupAssetTypeColumnWithIcons() {
        assetTypeColumn.setCellFactory(col -> new TableCell<URLValidationResult, String>() {
            @Override
            protected void updateItem(String assetType, boolean empty) {
                super.updateItem(assetType, empty);
                
                if (empty || assetType == null || assetType.isEmpty()) {
                    setText(null);
                    setBackground(Background.EMPTY);
                    return;
                }
                
                // Format the asset type for display with proper capitalization
                String displayText = formatAssetTypeForDisplay(assetType);
                setText(displayText);
                
                // Set background color based on content type
                BackgroundFill fill;
                if (assetType.equals("webpage")) {
                    fill = new BackgroundFill(Color.rgb(220, 220, 255), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("image")) {
                    fill = new BackgroundFill(Color.rgb(200, 255, 220), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("document")) {
                    fill = new BackgroundFill(Color.rgb(255, 230, 200), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("data")) {
                    fill = new BackgroundFill(Color.rgb(220, 255, 255), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("video") || assetType.equals("audio") || assetType.equals("multimedia")) {
                    fill = new BackgroundFill(Color.rgb(255, 200, 255), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("article")) {
                    fill = new BackgroundFill(Color.rgb(230, 240, 255), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("commentary") || assetType.equals("opinion-section")) {
                    fill = new BackgroundFill(Color.rgb(255, 235, 235), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("homepage")) {
                    fill = new BackgroundFill(Color.rgb(255, 255, 200), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.contains("-section")) {
                    fill = new BackgroundFill(Color.rgb(220, 255, 220), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.contains("-page") || assetType.contains("-archive")) {
                    fill = new BackgroundFill(Color.rgb(235, 235, 255), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("feature")) {
                    fill = new BackgroundFill(Color.rgb(255, 220, 180), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("press-release")) {
                    fill = new BackgroundFill(Color.rgb(200, 255, 200), new CornerRadii(3), Insets.EMPTY);
                } else if (assetType.equals("event")) {
                    fill = new BackgroundFill(Color.rgb(255, 200, 200), new CornerRadii(3), Insets.EMPTY);
                } else {
                    fill = new BackgroundFill(Color.rgb(240, 240, 240), new CornerRadii(3), Insets.EMPTY);
                }
                
                setBackground(new Background(fill));
                
                // Add tooltip with more information
                Tooltip tooltip = new Tooltip("Content type: " + displayText);
                setTooltip(tooltip);
            }
        });
    }
    
    /**
     * Formats asset type strings for display by capitalizing and formatting.
     */
    private String formatAssetTypeForDisplay(String assetType) {
        if (assetType == null || assetType.isEmpty()) {
            return "Unknown";
        }
        
        // Replace hyphens with spaces
        String formatted = assetType.replace('-', ' ');
        
        // Special cases for SpaceNews content types
        if (assetType.equals("homepage")) {
            return "Homepage";
        } else if (assetType.equals("article")) {
            return "News Article";
        } else if (assetType.equals("commentary")) {
            return "Commentary/Op-Ed";
        } else if (assetType.equals("feature")) {
            return "Feature Article";
        } else if (assetType.equals("press-release")) {
            return "Press Release";
        } else if (assetType.equals("multimedia")) {
            return "Multimedia";
        } else if (assetType.equals("event")) {
            return "Event Page";
        } else if (assetType.equals("civil-space-section")) {
            return "Civil Space Section";
        } else if (assetType.equals("commercial-space-section")) {
            return "Commercial Space Section";
        } else if (assetType.equals("military-space-section")) {
            return "Military Space Section";
        } else if (assetType.equals("launch-section")) {
            return "Launch Section";
        } else if (assetType.equals("opinion-section")) {
            return "Opinion Section";
        } else if (assetType.equals("category-page")) {
            return "Category Page";
        } else if (assetType.equals("author-page")) {
            return "Author Page";
        } else if (assetType.equals("tag-page")) {
            return "Tag Page";
        } else if (assetType.contains("-archive")) {
            return "Archive Page";
        }
        
        // For general asset types
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : formatted.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Loads site map projects from the service.
     */
    private void loadSiteMapProjects() {
        try {
            // Clear current projects
            siteMapProjects.clear();
            
            // Load projects from service
            List<com.spacedataarchive.model.SiteMapProject> projects = siteMapService.listProjects();
            
            logger.info("Found {} site map projects", projects.size());
            
            // Add to observable list
            for (com.spacedataarchive.model.SiteMapProject project : projects) {
                siteMapProjects.add(new SiteMapProject(project.getId(), project.getName()));
                logger.debug("Added project: {} (ID: {})", project.getName(), project.getId());
            }
            
            if (!siteMapProjects.isEmpty()) {
                // Select first project
                logger.info("Setting default selection to first project");
                Platform.runLater(() -> {
                    siteMapComboBox.getSelectionModel().selectFirst();
                });
            } else {
                updateStatus("No site maps available. Create a site map first.");
                logger.warn("No site maps available");
            }
        } catch (Exception e) {
            logger.error("Failed to load site map projects", e);
            updateStatus("Error loading site maps: " + e.getMessage());
        }
    }
    
    /**
     * Loads URLs from the selected site map.
     * 
     * @param siteMapId The ID of the site map
     */
    private void loadUrlsFromSiteMap(String siteMapId) {
        // Show loading indicator
        updateStatus("Loading URLs from site map...");
        
        // Disable UI elements during loading
        Platform.runLater(() -> {
            validateButton.setDisable(true);
        });
        
        // Use a CompletableFuture for better thread management
        CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Loading URLs from site map: {}", siteMapId);
                
                // Check if metadata indicates this is a large dataset
                Map<String, Object> metadata = siteMapService.getCrawlMetadata(siteMapId);
                boolean isLargeDataset = metadata != null && metadata.containsKey("totalChunks");
                
                if (isLargeDataset) {
                    logger.info("Large dataset detected - using optimized loading");
                    // For large datasets, just get the count without loading all URLs
                    Integer totalCount = metadata.containsKey("totalResults") ? 
                                        (Integer) metadata.get("totalResults") : null;
                    
                    if (totalCount == null) {
                        // If totalResults not in metadata, load first chunk to estimate
                        List<CrawlResult> firstChunk = siteMapService.getResultsChunk(siteMapId, 0);
                        Integer chunkSize = (Integer) metadata.getOrDefault("chunkSize", 50000);
                        Integer totalChunks = (Integer) metadata.getOrDefault("totalChunks", 10);
                        
                        // Estimate total based on chunk size and total chunks
                        totalCount = firstChunk != null ? 
                                    Math.min(firstChunk.size() + (chunkSize * (totalChunks - 1)), 1000000) : 0;
                    }
                    
                    // Return count and empty list to indicate large dataset special handling
                    return new LoadResult(totalCount, new ArrayList<>(), true);
                }
                
                // For normal datasets, proceed with regular loading
                List<CrawlResult> crawlResults = siteMapService.getCrawlResults(siteMapId);
                
                if (crawlResults == null || crawlResults.isEmpty()) {
                    logger.warn("No crawl results found for site map: {}", siteMapId);
                    return new LoadResult(0, new ArrayList<>(), false);
                }
                
                // Extract URLs
                List<String> urls = new ArrayList<>(crawlResults.size());
                for (CrawlResult result : crawlResults) {
                    urls.add(result.getUrl());
                }
                
                return new LoadResult(urls.size(), urls, false);
                
            } catch (Exception e) {
                logger.error("Error loading URLs from site map", e);
                return new LoadResult(-1, new ArrayList<>(), false); // Error indicator
            }
        }).thenAcceptAsync(result -> {
            try {
                // Update UI on JavaFX thread
                final SiteMapProject selectedProject = siteMapComboBox.getValue();
                
                if (result.getCount() <= 0) {
                    // No URLs or error
                    urlCountLabel.setText("0");
                    queueTotalLabel.setText("0");
                    urlSourceLabel.setText("No URLs available");
                    
                    if (result.getCount() < 0) {
                        // Error case
                        updateStatus("Error loading URLs from site map");
                    } else {
                        // Empty case
                        updateStatus("No URLs found for the selected site map.");
                        
                        // Show warning dialog
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("No URLs Found");
                        alert.setHeaderText("No URLs Found");
                        alert.setContentText("The selected site map does not contain any URLs. Please select another site map or crawl this one first.");
                        alert.show();
                    }
                } else {
                    // Success case
                    int urlCount = result.getCount();
                    
                    // Update stats
                    urlCountLabel.setText(String.valueOf(urlCount));
                    queueTotalLabel.setText(String.valueOf(urlCount));
                    urlSourceLabel.setText(selectedProject != null ? selectedProject.getName() : "Unknown");
                    
                    if (result.isLargeDataset()) {
                        updateStatus("Loaded metadata for " + formatLargeNumber(urlCount) + " URLs (large dataset)");
                        
                        // For large datasets, we'll load URLs only when validation starts
                        loadedUrls = null; // Set to null to indicate deferred loading
                        loadedDatasetId = siteMapId; // Store ID for deferred loading
                    } else {
                        updateStatus("Loaded " + urlCount + " URLs from site map.");
                        
                        // Store the URLs for later use
                        loadedUrls = result.getUrls();
                    }
                }
            } finally {
                // Re-enable UI elements
                validateButton.setDisable(false);
            }
        }, Platform::runLater);
    }
    
    /**
     * Helper class to store URL loading results
     */
    private static class LoadResult {
        private final int count;
        private final List<String> urls;
        private final boolean largeDataset;
        
        public LoadResult(int count, List<String> urls, boolean largeDataset) {
            this.count = count;
            this.urls = urls;
            this.largeDataset = largeDataset;
        }
        
        public int getCount() {
            return count;
        }
        
        public List<String> getUrls() {
            return urls;
        }
        
        public boolean isLargeDataset() {
            return largeDataset;
        }
    }
    
    /**
     * Formats large numbers with K/M suffixes for better readability
     */
    private String formatLargeNumber(int number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
    
    /**
     * Creates a page for the pagination control.
     * 
     * @param pageIndex The page index
     * @return A node containing the page content
     */
    private javafx.scene.Node createPage(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, allValidationResults.size());
        
        validationResults.clear();
        if (fromIndex < allValidationResults.size()) {
            validationResults.addAll(
                FXCollections.observableArrayList(
                    allValidationResults.subList(fromIndex, toIndex)
                )
            );
        }
        
        return resultsTableView;
    }
    
    /**
     * Displays the validation results in the table with pagination.
     * 
     * @param collection The validated URL collection
     */
    private void displayValidationResults(ValidatedUrlCollection collection) {
        // Clear current results
        allValidationResults.clear();
        validationResults.clear();
        
        if (collection == null || collection.getUrls() == null) {
            return;
        }
        
        // Add results to the full results list
        for (EnhancedValidationResult result : collection.getUrls()) {
            allValidationResults.add(new URLValidationResult(
                result.getUrl(),
                result.getStatus(),
                result.getMessage()
            ));
        }
        
        // Update pagination
        final int pageCount = Math.max(1, (int) Math.ceil((double) allValidationResults.size() / ROWS_PER_PAGE));
        
        if (resultsPagination != null) {
            Platform.runLater(() -> {
                resultsPagination.setPageCount(pageCount);
                resultsPagination.setCurrentPageIndex(0);
                resultsPagination.setVisible(pageCount > 1);
            });
        } else {
            // No pagination control available, load all results directly
            validationResults.addAll(allValidationResults);
        }
    }
    
    /**
     * Generates an AI summary of the validation results.
     * 
     * @param collection The validated URL collection
     */
    private void generateAISummary(ValidatedUrlCollection collection) {
        if (collection == null) {
            return;
        }
        
        // Create summary message
        StringBuilder summary = new StringBuilder();
        summary.append("I analyzed all URLs. Results:\n");
        summary.append("- ").append(collection.getValidUrls()).append(" URLs are valid and contain relevant content\n");
        summary.append("- ").append(collection.getWarningUrls()).append(" URLs have warnings (redirects or minor issues)\n");
        summary.append("- ").append(collection.getInvalidUrls()).append(" URLs have errors or are invalid\n\n");
        
        // Add metadata analysis if available
        int imageCount = 0;
        int documentCount = 0;
        int videoCount = 0;
        int dataCount = 0;
        
        for (EnhancedValidationResult result : collection.getUrls()) {
            String assetType = result.getAssetType();
            if (assetType != null) {
                switch (assetType) {
                    case "image": imageCount++; break;
                    case "document": documentCount++; break;
                    case "video": videoCount++; break;
                    case "data": dataCount++; break;
                    default: break;
                }
            }
        }
        
        if (imageCount > 0 || documentCount > 0 || videoCount > 0 || dataCount > 0) {
            summary.append("Content analysis:\n");
            if (imageCount > 0) summary.append("- ").append(imageCount).append(" image resources\n");
            if (documentCount > 0) summary.append("- ").append(documentCount).append(" document resources\n");
            if (videoCount > 0) summary.append("- ").append(videoCount).append(" video resources\n");
            if (dataCount > 0) summary.append("- ").append(dataCount).append(" data resources\n");
            summary.append("\n");
        }
        
        summary.append("Would you like detailed security recommendations for the flagged URLs?");
        
        // Add message to UI
        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, summary.toString()));
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
                String response = "I can help validate these URLs for compliance with archival standards. " +
                                  "I'll check for security concerns, content relevance, and technical issues. " +
                                  "Would you like me to run a complete validation or focus on a particular aspect?";
                
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
     * Adds a placeholder conversation to demonstrate AI functionality.
     */
    private void addPlaceholderConversation() {
        // Add a welcome message
        addMessageToUI(new AIMessage(AIMessage.Role.SYSTEM, 
            "Welcome to the URL Validation AI Assistant. I can help you validate URLs for security, " +
            "relevance, and technical compliance with archival standards."
        ));
        
        // Add a sample conversation
        addMessageToUI(new AIMessage(AIMessage.Role.USER, 
            "Can you check if these URLs conform to our archival standards?"
        ));
        
        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, 
            "I analyzed 28 URLs. 23 are valid, 3 contain potential security concerns (insecure protocols), " +
            "and 2 have broken redirects. Would you like a detailed report?"
        ));
    }
    
    /**
     * Adds a message to the chat UI.
     * 
     * @param message The message to add
     */
    private void addMessageToUI(AIMessage message) {
        VBox messageBox = new VBox();
        messageBox.setSpacing(5);
        
        // Style based on message role
        Background background = switch (message.getRole()) {
            case USER -> new Background(new BackgroundFill(Color.rgb(230, 230, 230), new CornerRadii(10), Insets.EMPTY));
            case SYSTEM -> new Background(new BackgroundFill(Color.rgb(220, 240, 255), new CornerRadii(10), Insets.EMPTY));
            case ASSISTANT -> new Background(new BackgroundFill(Color.rgb(240, 230, 255), new CornerRadii(10), Insets.EMPTY));
            default -> new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10), Insets.EMPTY));
        };
        
        HBox header = new HBox();
        header.setSpacing(10);
        header.setPadding(new Insets(5, 10, 0, 10));
        
        Label roleLabel = new Label(formatRole(message.getRole()));
        roleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a4a4a;");
        
        header.getChildren().add(roleLabel);
        
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setPadding(new Insets(0, 10, 5, 10));
        
        messageBox.getChildren().addAll(header, contentLabel);
        messageBox.setBackground(background);
        messageBox.setPadding(new Insets(0, 0, 0, message.getRole() == AIMessage.Role.USER ? 50 : 0));
        
        Platform.runLater(() -> {
            chatContainer.getChildren().add(messageBox);
            
            // Scroll to bottom
            chatScrollPane.setVvalue(1.0);
        });
    }
    
    /**
     * Formats a role for display.
     * 
     * @param role The message role
     * @return The formatted role
     */
    private String formatRole(AIMessage.Role role) {
        return switch (role) {
            case USER -> "You";
            case SYSTEM -> "System";
            case ASSISTANT -> "AI Assistant";
            default -> role.toString();
        };
    }
    
    /**
     * Updates the status label.
     * 
     * @param status The status message
     */
    private void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
        logger.info(status);
    }
    
    /**
     * Updates the status label with validation count information.
     * 
     * @param validCount Number of valid URLs
     * @param invalidCount Number of invalid URLs
     * @param warningCount Number of URLs with warnings
     * @param totalCount Total number of URLs
     */
    private void updateValidationStatus(int validCount, int invalidCount, int warningCount, int totalCount) {
        // Update validation progress
        int progress = totalCount > 0 ? (validCount + invalidCount + warningCount) * 100 / totalCount : 0;
        
        validationProgressBar.setProgress(totalCount > 0 ? (double)(validCount + invalidCount + warningCount) / totalCount : 0);
        progressLabel.setText(progress + "%");
        
        // Update status text with counts
        statusLabel.setText(String.format("URLs Validated: %d valid, %d warnings, %d invalid (of %d total)", 
                                        validCount, warningCount, invalidCount, totalCount));
        
        // Count different content types for SpaceNews URLs
        int articleCount = 0;
        int commentaryCount = 0;
        int featureCount = 0;
        int otherCount = 0;
        
        for (URLValidationResult result : allValidationResults) {
            String assetType = result.getAssetType();
            if (assetType != null) {
                if (assetType.equals("article")) {
                    articleCount++;
                } else if (assetType.equals("commentary")) {
                    commentaryCount++;
                } else if (assetType.equals("feature")) {
                    featureCount++;
                } else if (!assetType.equals("webpage") && !assetType.equals("image") && 
                          !assetType.equals("video") && !assetType.equals("audio") && 
                          !assetType.equals("document") && !assetType.equals("data")) {
                    otherCount++;
                }
            }
        }
        
        // If we have SpaceNews content types, display a summary in the status container
        if (articleCount > 0 || commentaryCount > 0 || featureCount > 0 || otherCount > 0) {
            // Create a label for SpaceNews content types if not already added
            if (statusContainer.getChildren().size() < 2) {
                Label spaceNewsLabel = new Label();
                spaceNewsLabel.setId("spaceNewsContentLabel");
                statusContainer.getChildren().add(spaceNewsLabel);
            }
            
            // Find the SpaceNews label and update it
            for (javafx.scene.Node node : statusContainer.getChildren()) {
                if (node instanceof Label && "spaceNewsContentLabel".equals(node.getId())) {
                    Label spaceNewsLabel = (Label) node;
                    spaceNewsLabel.setText(String.format("SpaceNews Content: %d articles, %d commentaries, %d features, %d other",
                                                      articleCount, commentaryCount, featureCount, otherCount));
                    break;
                }
            }
        }
    }
    
    /**
     * Class for URL validation results in the table view.
     */
    public static class URLValidationResult {
        private final javafx.beans.property.StringProperty url;
        private final javafx.beans.property.StringProperty status;
        private final javafx.beans.property.StringProperty message;
        private final javafx.beans.property.StringProperty title;
        private final javafx.beans.property.StringProperty assetType;
        
        public URLValidationResult(String url, String status, String message) {
            this.url = new SimpleStringProperty(url);
            this.status = new SimpleStringProperty(status);
            this.message = new SimpleStringProperty(message);
            this.title = new SimpleStringProperty("");
            this.assetType = new SimpleStringProperty("");
        }
        
        public URLValidationResult(String url, String status, String message, String title, String assetType) {
            this.url = new SimpleStringProperty(url);
            this.status = new SimpleStringProperty(status);
            this.message = new SimpleStringProperty(message);
            this.title = new SimpleStringProperty(title != null ? title : "");
            this.assetType = new SimpleStringProperty(assetType != null ? assetType : "");
        }
        
        public javafx.beans.property.StringProperty urlProperty() {
            return url;
        }
        
        public javafx.beans.property.StringProperty statusProperty() {
            return status;
        }
        
        public javafx.beans.property.StringProperty messageProperty() {
            return message;
        }
        
        public javafx.beans.property.StringProperty titleProperty() {
            return title;
        }
        
        public javafx.beans.property.StringProperty assetTypeProperty() {
            return assetType;
        }
        
        public String getUrl() {
            return url.get();
        }
        
        public String getStatus() {
            return status.get();
        }
        
        public String getTitle() {
            return title.get();
        }
        
        public String getAssetType() {
            return assetType.get();
        }
    }
    
    /**
     * Starts the validation process.
     */
    private void startValidation() {
        if (validationActive.get()) {
            updateStatus("Validation already in progress");
            return;
        }
        
        // Initialize the results table
        Platform.runLater(() -> {
            allValidationResults.clear();
            validationResults.clear();
            if (resultsPagination != null) {
                resultsPagination.setPageCount(1);
                resultsPagination.setCurrentPageIndex(0);
                resultsPagination.setVisible(false);
            }
        });

        // Check if we have a loaded dataset ID with deferred loading
        if ((loadedUrls == null || loadedUrls.isEmpty()) && loadedDatasetId != null) {
            updateStatus("Loading URLs from large dataset...");
            
            // For large datasets, load a reasonable batch of URLs for validation
            try {
                List<CrawlResult> firstChunk = siteMapService.getResultsChunk(loadedDatasetId, 0);
                if (firstChunk != null && !firstChunk.isEmpty()) {
                    // Extract URLs from the first chunk
                    List<String> urls = new ArrayList<>(firstChunk.size());
                    for (CrawlResult result : firstChunk) {
                        urls.add(result.getUrl());
                    }
                    loadedUrls = urls;
                    updateStatus("Loaded " + urls.size() + " URLs from large dataset for validation");
                }
            } catch (Exception e) {
                logger.error("Error loading URLs chunk for validation", e);
                updateStatus("Error loading URLs: " + e.getMessage());
                return;
            }
        }
        
        // Check if we have URLs to validate
        if (loadedUrls == null || loadedUrls.isEmpty()) {
            updateStatus("No URLs to validate. Please select a site map first.");
            
            // Show error dialog
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No URLs");
                alert.setHeaderText("No URLs to Validate");
                alert.setContentText("Please select a site map first to load URLs for validation.");
                alert.showAndWait();
            });
            
            return;
        }
        
        // Get selected site map
        SiteMapProject selectedSiteMap = siteMapComboBox.getSelectionModel().getSelectedItem();
        if (selectedSiteMap == null) {
            updateStatus("No site map selected");
            return;
        }
        
        // Get validation method
        String method = validationMethodComboBox.getValue();
        boolean useAI = "AI-assisted Validation".equals(method) || "Enhanced Validation".equals(method);
        
        // Update UI to show we're starting
        validateButton.setDisable(true);
        stopButton.setDisable(false);
        validationActive.set(true);
        updateStatus("Preparing validation...");
        
        // Process in background thread to avoid UI freezing
        new Thread(() -> {
            try {
                // Create validation collection
                String validationName = "Validation of " + selectedSiteMap.getName() + " - " + 
                                      LocalDateTime.now().format(DATE_FORMATTER);
                
                // Log validation start
                logger.info("Starting validation for site map: {} (ID: {})", selectedSiteMap.getName(), selectedSiteMap.getId());
                logger.info("Validation method: {}", method);
                logger.info("Validating {} URLs", loadedUrls.size());
                
                if (loadedUrls.isEmpty()) {
                    Platform.runLater(() -> {
                        updateStatus("No valid URLs found to validate");
                        validateButton.setDisable(false);
                        stopButton.setDisable(true);
                        validationActive.set(false);
                    });
                    return;
                }
                
                // Create validation collection with explicit URLs
                final ValidatedUrlCollection collection = validationService.createValidationWithUrls(
                    selectedSiteMap.getId(), validationName, loadedUrls);
                
                if (collection == null) {
                    logger.error("Failed to create validation collection");
                    Platform.runLater(() -> {
                        updateStatus("Failed to create validation collection");
                        validateButton.setDisable(false);
                        stopButton.setDisable(true);
                        validationActive.set(false);
                    });
                    return;
                }
                
                // Store validation ID
                currentValidationId = collection.getId();
                logger.info("Created validation collection with ID: {}", currentValidationId);
                
                // Add a verification step to ensure the collection is saved
                // Try to load it once to confirm storage saved it correctly
                try {
                    // Small delay to ensure file system has completed the write
                    Thread.sleep(200);
                    
                    // Verify the collection exists by trying to load it
                    ValidatedUrlCollection verifyCollection = validationService.getValidationCollection(currentValidationId);
                    if (verifyCollection == null) {
                        logger.error("Validation collection wasn't properly saved: {}", currentValidationId);
                        throw new IllegalStateException("Validation collection wasn't properly saved");
                    }
                    logger.info("Verified validation collection was saved correctly: {}", currentValidationId);
                } catch (Exception e) {
                    logger.error("Error verifying validation collection: {}", e.getMessage());
                    Platform.runLater(() -> {
                        updateStatus("Error saving validation collection: " + e.getMessage());
                        validateButton.setDisable(false);
                        stopButton.setDisable(true);
                        validationActive.set(false);
                    });
                    return;
                }
                
                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    // Clear previous results
                    validationResults.clear();
                    
                    // Add AI message if using AI
                    if (useAI) {
                        String aiPrompt = "I'll validate the URLs from '" + selectedSiteMap.getName() + 
                                         "' using advanced security and content relevance checks. Processing...";
                        addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, aiPrompt));
                    }
                    
                    // Start validation
                    if (loadedUrls.size() > 100000) {
                        // Show info alert for large datasets
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Large Dataset");
                            alert.setHeaderText("Processing Large Dataset");
                            alert.setContentText("You are validating " + loadedUrls.size() + 
                               " URLs. Progress bar updates may be slow initially, and the application will save results periodically " +
                               "rather than after every validation to improve performance. Please be patient.");
                            alert.show();
                            
                            // Update status
                            updateStatus("Starting validation of " + loadedUrls.size() + " URLs (large dataset) from site map: " + 
                                        selectedSiteMap.getName());
                        });
                    } else {
                        // Update status for normal datasets
                        updateStatus("Starting validation of " + loadedUrls.size() + " URLs from site map: " + selectedSiteMap.getName());
                    }
                    
                    // Start validation
                    currentValidationFuture = validationService.startValidation(
                        currentValidationId,
                        10,  // concurrent requests
                        useAI
                    );
                    
                    // Set up a periodic task to update UI with new results
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    final ScheduledFuture<?> updateTask = scheduler.scheduleAtFixedRate(() -> {
                        try {
                            if (!validationActive.get()) {
                                return; // Don't update if validation is no longer active
                            }
                            
                            final ValidatedUrlCollection currentResults = 
                                validationService.getValidationCollection(currentValidationId);
                            
                            if (currentResults != null && currentResults.getUrls() != null) {
                                Platform.runLater(() -> {
                                    updateTableWithNewResults(currentResults);
                                });
                            }
                        } catch (Exception e) {
                            logger.error("Error updating results table", e);
                        }
                    }, 1, 2, TimeUnit.SECONDS); // Update every 2 seconds
                    
                    // Start progress update thread
                    startProgressUpdateThread();
                    
                    // Handle completion
                    currentValidationFuture.whenComplete((result, ex) -> {
                        // Cancel the update task
                        updateTask.cancel(false);
                        scheduler.shutdown();
                        
                        Platform.runLater(() -> {
                            if (ex != null) {
                                logger.error("Validation failed", ex);
                                updateStatus("Validation failed: " + ex.getMessage());
                                
                                // Add AI message if using AI
                                if (useAI) {
                                    addMessageToUI(new AIMessage(AIMessage.Role.ASSISTANT, 
                                        "I encountered an error while validating URLs: " + ex.getMessage()));
                                }
                            } else {
                                // Final refresh of results
                                updateTableWithNewResults(result);
                                
                                // Explicitly load results to ensure table is populated
                                loadAndDisplayValidationResults(currentValidationId);
                                
                                // Generate AI summary if using AI
                                if (useAI) {
                                    generateAISummary(result);
                                }
                                
                                updateValidationStatus(
                                    result.getValidUrls(),
                                    result.getInvalidUrls(),
                                    result.getWarningUrls(),
                                    result.getTotalUrls()
                                );
                            }
                            
                            // Reset UI state
                            validateButton.setDisable(false);
                            stopButton.setDisable(true);
                            validationActive.set(false);
                        });
                    });
                });
                
            } catch (Exception e) {
                logger.error("Error starting validation", e);
                Platform.runLater(() -> {
                    updateStatus("Error starting validation: " + e.getMessage());
                    validateButton.setDisable(false);
                    stopButton.setDisable(true);
                    validationActive.set(false);
                });
            }
        }).start();
    }
    
    /**
     * Updates the table with new validation results.
     * 
     * @param collection The validated URL collection
     */
    private void updateTableWithNewResults(ValidatedUrlCollection collection) {
        if (collection == null) {
            logger.warn("Attempted to update table with null collection");
            return;
        }
        
        // Log status data for debugging
        logger.info("Updating table with collection: id={}, status={}, total={}, urls size={}",
                   collection.getId(),
                   collection.getStatus(),
                   collection.getTotalUrls(),
                   collection.getUrls() != null ? collection.getUrls().size() : 0);
        
        // Check if we have results to display
        if (collection.getUrls() == null || collection.getUrls().isEmpty()) {
            logger.warn("Collection has no URLs to display");
            
            // If we have totalUrls but no URLs array, it might be a loading issue
            if (collection.getTotalUrls() > 0) {
                logger.info("Collection has totalUrls={} but empty URLs array. Attempting to refresh data.", 
                           collection.getTotalUrls());
                
                // Try to explicitly load validation results
                CompletableFuture.runAsync(() -> {
                    try {
                        // Wait a moment in case data is still being written
                        Thread.sleep(500);
                        
                        // Attempt to load results explicitly
                        if (currentValidationId != null) {
                            Platform.runLater(() -> loadAndDisplayValidationResults(currentValidationId));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Interrupted while waiting for data refresh");
                    }
                });
                
                // Show a warning in the UI
                Platform.runLater(() -> {
                    if (validationResults.isEmpty()) {
                        // Add a placeholder row to indicate data is being processed
                        URLValidationResult placeholderResult = new URLValidationResult(
                            "Data is being processed...",
                            "In Progress",
                            "Results are being saved but not yet visible. Please wait or click 'Reload Results'"
                        );
                        validationResults.add(placeholderResult);
                        
                        // Add a reload button to the UI if it doesn't exist
                        addReloadButton();
                    }
                });
            }
            return;
        }
        
        // If we have any data at all, remove the placeholder row if it exists
        Platform.runLater(() -> {
            validationResults.removeIf(result -> 
                "Data is being processed...".equals(result.getUrl()) && 
                "In Progress".equals(result.getStatus()));
        });
        
        int currentSize = allValidationResults.size();
        int collectionSize = collection.getUrls().size();
        int newResultsCount = collectionSize - currentSize;
        
        logger.info("Current results: {}, Collection results: {}, New results: {}", 
                  currentSize, collectionSize, newResultsCount);
        
        // Process all results when the collection is small or there are significant new results
        if (newResultsCount > 100 || allValidationResults.isEmpty() || collectionSize < 1000) {
            logger.info("Reprocessing all {} results in the collection", collectionSize);
            
            // Clear existing results
            allValidationResults.clear();
            
            // Convert all results to UI format
            List<URLValidationResult> uiResults = collection.getUrls().stream()
                .map(result -> new URLValidationResult(
                    result.getUrl(),
                    result.getStatus(),
                    result.getMessage(),
                    result.getUrlName(),
                    result.getAssetType()
                ))
                .collect(Collectors.toList());
                
            // Add all results
            allValidationResults.addAll(uiResults);
            
            // Update pagination
            final int pageCount = Math.max(1, (int) Math.ceil((double) allValidationResults.size() / ROWS_PER_PAGE));
            
            // Update the display
            Platform.runLater(() -> {
                validationResults.clear();
                int toIndex = Math.min(ROWS_PER_PAGE, allValidationResults.size());
                validationResults.addAll(FXCollections.observableArrayList(
                    allValidationResults.subList(0, toIndex)
                ));
                
                if (resultsPagination != null) {
                    resultsPagination.setPageCount(pageCount);
                    resultsPagination.setCurrentPageIndex(0);
                    resultsPagination.setVisible(pageCount > 1);
                }
                
                // Update validation status with counts from the collection
                updateValidationStatus(
                    collection.getValidUrls(),
                    collection.getInvalidUrls(),
                    collection.getWarningUrls(),
                    collection.getTotalUrls()
                );
            });
            
            return;
        }
        
        // Continue with existing code for processing new results...
        if (newResultsCount > 0) {
            List<EnhancedValidationResult> newResults = collection.getUrls()
                .subList(currentSize, collection.getUrls().size());
            
            // Convert to UI results
            List<URLValidationResult> uiResults = newResults.stream()
                .map(result -> new URLValidationResult(
                    result.getUrl(),
                    result.getStatus(),
                    result.getMessage(),
                    result.getUrlName(),  // Use the URL name as the title
                    result.getAssetType() // Include asset type
                ))
                .collect(Collectors.toList());
            
            // Add to full results list
            allValidationResults.addAll(uiResults);
            
            // Update pagination
            final int pageCount = Math.max(1, (int) Math.ceil((double) allValidationResults.size() / ROWS_PER_PAGE));
            int currentPageIndex = resultsPagination != null ? resultsPagination.getCurrentPageIndex() : 0;
            
            // Update the current page if needed
            if (currentPageIndex == pageCount - 2 && pageCount > 1) {
                // We're on the last page, update items directly
                validationResults.addAll(uiResults);
            } else if (currentPageIndex == 0) {
                // Initial page, update displayed items
                int toIndex = Math.min(ROWS_PER_PAGE, allValidationResults.size());
                validationResults.clear();
                validationResults.addAll(FXCollections.observableArrayList(
                    allValidationResults.subList(0, toIndex)
                ));
            }
            
            // Update pagination control
            if (resultsPagination != null) {
                resultsPagination.setPageCount(pageCount);
                resultsPagination.setVisible(pageCount > 1);
            }
            
            // Log the update
            if (newResultsCount >= 100 || allValidationResults.size() % 1000 == 0) {
                logger.info("Updated results table with {} new items, total: {}", 
                           newResultsCount, allValidationResults.size());
            }
        }
    }
    
    /**
     * Adds a reload button to the UI to manually force reloading of results.
     */
    private void addReloadButton() {
        // Check if we already have a reload button
        if (reloadButton != null) {
            return;
        }
        
        // Create reload button
        reloadButton = new Button("Reload Results");
        reloadButton.setOnAction(e -> {
            if (currentValidationId != null) {
                updateStatus("Manually reloading validation results...");
                loadAndDisplayValidationResults(currentValidationId);
            } else {
                updateStatus("No validation ID available to reload");
            }
        });
        
        // Add to UI
        if (statusContainer != null) {
            Platform.runLater(() -> {
                statusContainer.getChildren().add(reloadButton);
            });
        }
    }
    
    /**
     * Stops the ongoing validation process.
     */
    private void stopValidation() {
        if (!validationActive.get() || currentValidationId == null) {
            logger.warn("Cannot stop validation: no active validation or validation ID is null");
            return;
        }
        
        logger.info("Attempting to stop validation with ID: {}", currentValidationId);
        
        try {
            // Disable the stop button to prevent multiple stops
            Platform.runLater(() -> {
                stopButton.setDisable(true);
                updateStatus("Attempting to stop validation...");
            });
            
            // Stop validation
            boolean stopped = validationService.stopValidation(currentValidationId);
            
            if (stopped) {
                logger.info("Successfully set stop flag for validation: {}", currentValidationId);
                
                // Set our local validationActive to false as well
                validationActive.set(false);
                
                // Create a cancellation task for the current validation future if it exists
                if (currentValidationFuture != null && !currentValidationFuture.isDone()) {
                    logger.info("Attempting to cancel validation future");
                    boolean canceled = currentValidationFuture.cancel(true);
                    logger.info("Validation future cancellation result: {}", canceled);
                }
                
                // Stop progress update thread
                if (progressUpdateThread != null && progressUpdateThread.isAlive()) {
                    logger.info("Interrupting progress update thread");
                    progressUpdateThread.interrupt();
                }
                
                Platform.runLater(() -> {
                    updateStatus("Validation stopping - please wait for in-progress tasks to complete...");
                    // Show a notification that stopping may take a moment
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Stopping Validation");
                    alert.setHeaderText("Validation Process Stopping");
                    alert.setContentText("The validation has been signaled to stop. It may take a moment for all in-progress validation tasks to complete.");
                    alert.show();
                    
                    // Reset UI state
                    new Thread(() -> {
                        try {
                            // Wait a short time for processing to stop
                            Thread.sleep(2000);
                            Platform.runLater(() -> {
                                validateButton.setDisable(false);
                                stopButton.setDisable(true);
                                updateStatus("Validation stopped by user");
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            logger.error("Interrupted while waiting for validation to stop", e);
                        }
                    }).start();
                });
            } else {
                logger.error("Failed to stop validation: {}", currentValidationId);
                Platform.runLater(() -> {
                    updateStatus("Failed to stop validation");
                    stopButton.setDisable(false);
                });
            }
        } catch (Exception e) {
            logger.error("Error stopping validation", e);
            Platform.runLater(() -> {
                updateStatus("Error stopping validation: " + e.getMessage());
                stopButton.setDisable(false);
            });
        }
    }
    
    /**
     * Starts a thread to update the progress bar.
     */
    private void startProgressUpdateThread() {
        // Stop existing thread if any
        if (progressUpdateThread != null && progressUpdateThread.isAlive()) {
            progressUpdateThread.interrupt();
        }
        
        // Initialize progress UI
        Platform.runLater(() -> {
            validationProgressBar.setProgress(0);
            progressLabel.setText("0%");
            if (performanceLabel != null) {
                performanceLabel.setText("0 URLs/sec");
            }
            
            // Log initialization of the progress bar
            logger.info("Initialized progress bar for validation {}", currentValidationId);
        });
        
        // Create new thread
        progressUpdateThread = new Thread(() -> {
            try {
                logger.info("Starting progress update thread for validation {}", currentValidationId);
                int lastProgress = -1;
                long lastUpdateTime = System.currentTimeMillis();
                
                while (validationActive.get() && !Thread.currentThread().isInterrupted()) {
                    // Get validation status
                    if (currentValidationId != null) {
                        Map<String, Object> status = validationService.getValidationStatus(currentValidationId);
                        
                        if (status != null && !status.isEmpty()) {
                            // Update progress
                            final Integer progress = (Integer) status.getOrDefault("progress", 0);
                            final Integer validated = (Integer) status.getOrDefault("validated", 0);
                            final Integer total = (Integer) status.getOrDefault("total", 0);
                            final String state = (String) status.getOrDefault("state", "");
                            final Double urlsPerSecond = (Double) status.getOrDefault("urlsPerSecond", 0.0);
                            
                            // Log significant progress changes
                            if (progress != lastProgress) {
                                long currentTime = System.currentTimeMillis();
                                if (progress % 5 == 0 || currentTime - lastUpdateTime > 10000) {
                                    logger.info("Validation progress: {}% ({}/{}) - State: {} - Speed: {:.2f} URLs/sec", 
                                              progress, validated, total, state, urlsPerSecond);
                                    lastUpdateTime = currentTime;
                                }
                                lastProgress = progress;
                            }
                            
                            // Update UI on the JavaFX thread
                            Platform.runLater(() -> {
                                try {
                                    double progressValue = progress / 100.0;
                                    validationProgressBar.setProgress(progressValue);
                                    progressLabel.setText(progress + "% (" + validated + "/" + total + ")");
                                    
                                    // Update performance label if available
                                    if (performanceLabel != null && urlsPerSecond != null) {
                                        String speedText = String.format("%.1f URLs/sec", urlsPerSecond);
                                        performanceLabel.setText(speedText);
                                    }
                                    
                                    // Also update queue stats
                                    urlCountLabel.setText(String.valueOf(validated));
                                    queueTotalLabel.setText(String.valueOf(total));
                                    
                                    // Update status to show "URLs Validated" if we have some validated URLs
                                    if (validated > 0) {
                                        updateStatus("URLs Validated: " + validated + " (in progress)");
                                    }
                                } catch (Exception e) {
                                    logger.error("Error updating UI from progress thread", e);
                                }
                            });
                            
                            // Check if validation is complete
                            if ("COMPLETED".equals(state) || "ERROR".equals(state) || "STOPPED".equals(state)) {
                                logger.info("Validation completed with state: {}", state);
                                break;
                            }
                        } else {
                            logger.warn("No validation status available for {}", currentValidationId);
                        }
                    }
                    
                    // Sleep before next update - shorter interval for more responsive UI
                    Thread.sleep(200);
                }
                
                logger.info("Progress update thread completed for validation {}", currentValidationId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Progress update thread interrupted");
            } catch (Exception e) {
                logger.error("Error updating validation progress", e);
            }
        });
        
        progressUpdateThread.setDaemon(true);
        progressUpdateThread.setName("ProgressUpdateThread-" + currentValidationId);
        progressUpdateThread.start();
        logger.info("Started progress update thread: {}", progressUpdateThread.getName());
    }
    
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
            logger.info("URL validation AI assistant drawer " + (isDrawerVisible ? "hidden" : "shown"));
        }
    }
    
    // Add field declarations for the drawer components
    @FXML private VBox aiAssistantDrawer;
    @FXML private VBox aiAssistantToggle;
    @FXML private Button toggleAIDrawerButton;
    @FXML private Button expandAIDrawerButton;
    @FXML private TextArea detailsTextArea;
    
    /**
     * Explicitly loads validation results from a validation ID and updates the UI.
     * Useful for cases where the regular update methods might miss results.
     * 
     * @param validationId The ID of the validation collection to load
     */
    private void loadAndDisplayValidationResults(String validationId) {
        if (validationId == null || validationId.isEmpty()) {
            logger.warn("Cannot load validation results: Invalid validation ID");
            return;
        }
        
        // Log that we're attempting an explicit load
        logger.info("Explicitly loading validation results for: {}", validationId);
        
        // Update status to inform user
        updateStatus("Loading validation results...");
        
        // Use CompletableFuture to load asynchronously
        CompletableFuture.supplyAsync(() -> {
            try {
                URLValidationService validationService = ServiceFactory.getInstance().getUrlValidationService();
                ValidatedUrlCollection collection = validationService.getValidationCollection(validationId);
                
                if (collection == null) {
                    logger.warn("Failed to load validation collection: {}", validationId);
                    return null;
                }
                
                // Log some details about what we've loaded
                logger.info("Loaded validation collection: id={}, status={}, total={}, urlsSize={}",
                           collection.getId(),
                           collection.getStatus(),
                           collection.getTotalUrls(),
                           collection.getUrls() != null ? collection.getUrls().size() : 0);
                           
                return collection;
            } catch (Exception e) {
                logger.error("Error loading validation collection: {}", validationId, e);
                return null;
            }
        }).thenAcceptAsync(collection -> {
            if (collection == null) {
                updateStatus("Failed to load validation results");
                return;
            }
            
            if (collection.getUrls() == null || collection.getUrls().isEmpty()) {
                logger.warn("No validation results found in collection: {}", validationId);
                updateStatus("No validation results found");
                
                // Show an alert to the user
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Results Found");
                alert.setHeaderText("No Validation Results");
                alert.setContentText("The validation collection doesn't contain any results. This might be because the validation process is still running or hasn't saved results yet.");
                alert.show();
                return;
            }
            
            // Use the existing displayValidationResults method to update the UI
            displayValidationResults(collection);
            
            // Update validation status with counts
            updateValidationStatus(
                collection.getValidUrls(),
                collection.getInvalidUrls(),
                collection.getWarningUrls(),
                collection.getTotalUrls()
            );
            
            // Log success
            logger.info("Successfully loaded and displayed {} validation results", collection.getUrls().size());
        }, Platform::runLater);
    }
} 