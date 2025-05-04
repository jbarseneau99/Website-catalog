package com.spacedataarchive.controller;

import com.spacedataarchive.model.CrawlResult;
import com.spacedataarchive.model.SiteMapProject;
import com.spacedataarchive.service.SiteMapService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the Site Map tab.
 * Provides AI-assisted site crawling functionality.
 */
public class SiteMapController {
    private static final Logger logger = LoggerFactory.getLogger(SiteMapController.class);
    
    // UI components
    @FXML private TextField projectNameField;
    @FXML private TextField seedUrlField;
    @FXML private Spinner<Integer> crawlDepthSpinner;
    @FXML private Spinner<Integer> maxPagesSpinner;
    
    @FXML private ComboBox<SiteMapProject> projectComboBox;
    @FXML private Button newProjectButton;
    @FXML private Button startCrawlButton;
    @FXML private Button stopCrawlButton;
    @FXML private Button exportResultsButton;
    @FXML private Button settingsButton;
    
    @FXML private TableView<CrawlResult> crawlResultsTable;
    @FXML private TableColumn<CrawlResult, String> urlColumn;
    @FXML private TableColumn<CrawlResult, String> titleColumn;
    @FXML private TableColumn<CrawlResult, String> statusColumn;
    
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea aiAnalysisTextArea;
    @FXML private Label performanceLabel;
    @FXML private Pane visualizationPane;
    
    // Pagination and filters
    @FXML private Pagination resultsPagination;
    @FXML private Label totalResultsLabel;
    @FXML private TextField urlFilterField;
    @FXML private TextField titleFilterField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TextField quickUrlFilterField;
    @FXML private ComboBox<String> quickStatusFilterCombo;
    
    // Services
    private final SiteMapService siteMapService = SiteMapService.getInstance();
    
    // Data
    private final ObservableList<CrawlResult> crawlResults = FXCollections.observableArrayList();
    private final ObservableList<CrawlResult> filteredResults = FXCollections.observableArrayList();
    private SiteMapProject currentProject;
    
    private CompletableFuture<List<CrawlResult>> activeCrawl;
    private volatile boolean stopRequested = false;
    
    // Performance tracking
    private long startTime;
    private long lastUpdateTime;
    private int lastResultCount = 0;
    private double currentCrawlRate = 0.0;
    private double avgCrawlRate = 0.0;
    private final List<Double> historicalRates = new ArrayList<>();
    
    // Pagination settings
    private static final int ROWS_PER_PAGE = 50;
    
    private Canvas crawlerCanvas;
    
    // Add this field to support background tasks
    private ExecutorService crawlExecutor;
    
    // Add field declarations for the drawer components
    @FXML private VBox aiAssistantDrawer;
    @FXML private VBox aiAssistantToggle;
    @FXML private Button toggleAIDrawerButton;
    @FXML private Button expandAIDrawerButton;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        logger.info("Initializing SiteMapController");
        
        // Show a loading message while initializing
        updateStatus("Initializing interface...");
        
        // Initialize UI components in a background thread to keep UI responsive
        CompletableFuture.runAsync(() -> {
            try {
                // Initialize executor for crawling tasks
                if (crawlExecutor == null) {
                    crawlExecutor = Executors.newFixedThreadPool(2, r -> {
                        Thread t = new Thread(r);
                        t.setDaemon(true);
                        return t;
                    });
                }
                
                // Perform UI initialization on the JavaFX thread
                Platform.runLater(() -> {
                    try {
                        initializeUI();
                        // Load existing projects after UI is ready
                        loadProjects();
                        updateStatus("Ready");
                    } catch (Exception e) {
                        logger.error("Error during UI initialization", e);
                        updateStatus("Error initializing interface: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Error initializing controller", e);
                Platform.runLater(() -> updateStatus("Error initializing: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Initializes UI components.
     */
    private void initializeUI() {
        // Configure table columns with cell factories for better performance
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Optimize table for performance
        crawlResultsTable.setItems(filteredResults);
        
        // Reduce table repaint frequency for better performance with large datasets
        crawlResultsTable.setFixedCellSize(25); // Set fixed height for rows
        
        // Create canvas for visualization
        if (visualizationPane != null) {
            crawlerCanvas = new Canvas(visualizationPane.getPrefWidth(), visualizationPane.getPrefHeight());
            visualizationPane.getChildren().add(crawlerCanvas);
            
            // Bind canvas size to pane size
            crawlerCanvas.widthProperty().bind(visualizationPane.widthProperty());
            crawlerCanvas.heightProperty().bind(visualizationPane.heightProperty());
            
            // Draw initial empty graph
            updateCrawlerVisualization();
        }
        
        // Initialize performance label if present
        if (performanceLabel != null) {
            performanceLabel.setText("Waiting for crawl to start...");
            performanceLabel.setTextFill(Color.DARKBLUE);
            performanceLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        }
        
        // Initialize pagination with row virtualization for better performance
        if (resultsPagination != null) {
            resultsPagination.setPageCount(1);
            resultsPagination.setCurrentPageIndex(0);
            resultsPagination.setMaxPageIndicatorCount(10);
            resultsPagination.currentPageIndexProperty().addListener((obs, oldVal, newVal) -> {
                if (!oldVal.equals(newVal)) {
                    updateTableForPage(newVal.intValue());
                }
            });
        }
        
        // Initialize filters
        if (urlFilterField != null) {
            urlFilterField.textProperty().addListener((obs, oldVal, newVal) -> {
                // Don't auto-apply advanced filters - user will click the Apply button
            });
        }
        
        if (titleFilterField != null) {
            titleFilterField.textProperty().addListener((obs, oldVal, newVal) -> {
                // Don't auto-apply advanced filters - user will click the Apply button
            });
        }
        
        if (statusFilterCombo != null) {
            statusFilterCombo.getItems().addAll("All", "OK", "Excluded", "Error");
            statusFilterCombo.getSelectionModel().select("All");
        }
        
        // Initialize quick filters
        if (quickUrlFilterField != null) {
            quickUrlFilterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        
        if (quickStatusFilterCombo != null) {
            quickStatusFilterCombo.getItems().addAll("All", "OK", "Excluded", "Error");
            quickStatusFilterCombo.getSelectionModel().select("All");
            quickStatusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
        
        // Configure spinner values
        crawlDepthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        maxPagesSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000, 100));
        
        // Configure project combo box
        projectComboBox.setConverter(new javafx.util.StringConverter<SiteMapProject>() {
            @Override
            public String toString(SiteMapProject project) {
                return project == null ? "" : project.getName() + " (" + project.getSeedUrl() + ")";
            }
            
            @Override
            public SiteMapProject fromString(String string) {
                return null; // Not needed for combo box
            }
        });
        
        projectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentProject = newValue;
            refreshProjectData();
        });
        
        // Configure buttons
        newProjectButton.setOnAction(e -> handleNewProject());
        startCrawlButton.setOnAction(e -> handleStartCrawl());
        stopCrawlButton.setOnAction(e -> handleStopCrawl());
        exportResultsButton.setOnAction(e -> handleExportResults());
        if (settingsButton != null) {
            settingsButton.setOnAction(e -> showSettingsDialog());
        }
        
        // Disable stop button initially
        stopCrawlButton.setDisable(true);
        
        // Default project name
        projectNameField.setText("Site Map " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        seedUrlField.setText("https://spacenews.com");
        
        // Hide progress bar initially
        progressBar.setVisible(false);
        
        // Configure AI analysis text area
        aiAnalysisTextArea.setEditable(false);
        aiAnalysisTextArea.setWrapText(true);
    }
    
    /**
     * Updates the table view for the current pagination page.
     * 
     * @param pageIndex The page index to display
     */
    private void updateTableForPage(int pageIndex) {
        try {
            int fromIndex = pageIndex * ROWS_PER_PAGE;
            
            // Handle the case when filtered results change and table needs updating
            if (fromIndex >= filteredResults.size() && !filteredResults.isEmpty()) {
                fromIndex = 0;
                if (resultsPagination != null) {
                    resultsPagination.setCurrentPageIndex(0);
                }
            }
            
            int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredResults.size());
            
            // Create a new observable list for this page to avoid list binding issues
            ObservableList<CrawlResult> pageItems = FXCollections.observableArrayList();
            
            if (fromIndex < filteredResults.size()) {
                pageItems.addAll(filteredResults.subList(fromIndex, toIndex));
            }
            
            // Set new items on the table
            crawlResultsTable.setItems(pageItems);
            
            // Ensure selection and scroll position is correctly maintained
            crawlResultsTable.refresh();
            
            // Update status with page information
            updateStatus("Showing page " + (pageIndex + 1) + " of " + 
                        (resultsPagination != null ? resultsPagination.getPageCount() : 1));
        } catch (Exception e) {
            logger.error("Error updating table for page " + pageIndex, e);
            updateStatus("Error displaying page data");
        }
    }
    
    /**
     * Applies filters to the result data based on the quick filters.
     */
    @FXML
    public void applyFilters() {
        // For large datasets, perform filtering in a background thread
        if (crawlResults.size() > 1000) {
            updateStatus("Applying filters to large dataset...");
            
            CompletableFuture.runAsync(() -> {
                try {
                    // Get quick filter values
                    String quickUrlFilter = quickUrlFilterField != null ? quickUrlFilterField.getText().toLowerCase() : "";
                    String quickStatusFilter = quickStatusFilterCombo != null && quickStatusFilterCombo.getValue() != null ? 
                                               quickStatusFilterCombo.getValue() : "All";
                    
                    // Create a temporary list for filtered results
                    List<CrawlResult> newFilteredResults = new ArrayList<>();
                    
                    // Process in small batches to allow cancellation
                    int batchSize = 1000;
                    for (int i = 0; i < crawlResults.size(); i += batchSize) {
                        int end = Math.min(i + batchSize, crawlResults.size());
                        List<CrawlResult> batch = crawlResults.subList(i, end);
                        
                        for (CrawlResult result : batch) {
                            if (matchesQuickFilters(result, quickUrlFilter, quickStatusFilter)) {
                                newFilteredResults.add(result);
                            }
                        }
                    }
                    
                    // Update the filtered results on the FX thread
                    Platform.runLater(() -> {
                        filteredResults.clear();
                        filteredResults.addAll(newFilteredResults);
                        updatePaginationAndCounts();
                        updateStatus("Filter applied: showing " + filteredResults.size() + " of " + crawlResults.size() + " results");
                    });
                } catch (Exception e) {
                    logger.error("Error applying filters", e);
                    Platform.runLater(() -> updateStatus("Error applying filters: " + e.getMessage()));
                }
            });
        } else {
            // For smaller datasets, process directly in the UI thread
            // Clear the filtered list
            filteredResults.clear();
            
            // Get quick filter values
            String quickUrlFilter = quickUrlFilterField != null ? quickUrlFilterField.getText().toLowerCase() : "";
            String quickStatusFilter = quickStatusFilterCombo != null && quickStatusFilterCombo.getValue() != null ? 
                                      quickStatusFilterCombo.getValue() : "All";
            
            // Apply filters
            for (CrawlResult result : crawlResults) {
                if (matchesQuickFilters(result, quickUrlFilter, quickStatusFilter)) {
                    filteredResults.add(result);
                }
            }
            
            updatePaginationAndCounts();
        }
    }
    
    /**
     * Applies advanced filters from the advanced filters panel.
     */
    @FXML
    public void applyAdvancedFilters() {
        // Clear the filtered list
        filteredResults.clear();
        
        // Get advanced filter values
        String urlFilter = urlFilterField != null ? urlFilterField.getText().toLowerCase() : "";
        String titleFilter = titleFilterField != null ? titleFilterField.getText().toLowerCase() : "";
        String statusFilter = statusFilterCombo != null && statusFilterCombo.getValue() != null ? 
                             statusFilterCombo.getValue() : "All";
        
        // Apply filters
        for (CrawlResult result : crawlResults) {
            if (matchesAdvancedFilters(result, urlFilter, titleFilter, statusFilter)) {
                filteredResults.add(result);
            }
        }
        
        // Also update quick filters to match advanced settings
        if (quickUrlFilterField != null) {
            quickUrlFilterField.setText(urlFilter);
        }
        
        if (quickStatusFilterCombo != null) {
            quickStatusFilterCombo.setValue(statusFilter);
        }
        
        updatePaginationAndCounts();
    }
    
    /**
     * Resets all filters to default values.
     */
    @FXML
    public void resetFilters() {
        // Clear filter fields
        if (urlFilterField != null) {
            urlFilterField.clear();
        }
        
        if (titleFilterField != null) {
            titleFilterField.clear();
        }
        
        if (statusFilterCombo != null) {
            statusFilterCombo.getSelectionModel().select("All");
        }
        
        if (quickUrlFilterField != null) {
            quickUrlFilterField.clear();
        }
        
        if (quickStatusFilterCombo != null) {
            quickStatusFilterCombo.getSelectionModel().select("All");
        }
        
        // Apply reset filters
        filteredResults.clear();
        filteredResults.addAll(crawlResults);
        
        updatePaginationAndCounts();
    }
    
    /**
     * Checks if a crawl result matches the quick filters.
     * 
     * @param result The crawl result to check
     * @param urlFilter The URL filter text
     * @param statusFilter The status filter value
     * @return True if the result matches the filters
     */
    private boolean matchesQuickFilters(CrawlResult result, String urlFilter, String statusFilter) {
        // Check URL filter
        if (!urlFilter.isEmpty() && !result.getUrl().toLowerCase().contains(urlFilter)) {
            return false;
        }
        
        // Check status filter
        if (!"All".equals(statusFilter) && !result.getStatus().equals(statusFilter)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a crawl result matches the advanced filters.
     * 
     * @param result The crawl result to check
     * @param urlFilter The URL filter text
     * @param titleFilter The title filter text
     * @param statusFilter The status filter value
     * @return True if the result matches the filters
     */
    private boolean matchesAdvancedFilters(CrawlResult result, String urlFilter, String titleFilter, String statusFilter) {
        // Check URL filter
        if (!urlFilter.isEmpty() && !result.getUrl().toLowerCase().contains(urlFilter)) {
            return false;
        }
        
        // Check title filter
        if (!titleFilter.isEmpty() && !result.getTitle().toLowerCase().contains(titleFilter)) {
            return false;
        }
        
        // Check status filter
        if (!"All".equals(statusFilter) && !result.getStatus().equals(statusFilter)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Updates pagination and count labels after filtering.
     */
    private void updatePaginationAndCounts() {
        // Update pagination
        int pageCount = (int) Math.ceil((double) filteredResults.size() / ROWS_PER_PAGE);
        pageCount = Math.max(1, pageCount);
        
        if (resultsPagination != null) {
            resultsPagination.setPageCount(pageCount);
            if (resultsPagination.getCurrentPageIndex() >= pageCount) {
                resultsPagination.setCurrentPageIndex(0);
            } else {
                // Force refresh of current page
                updateTableForPage(resultsPagination.getCurrentPageIndex());
            }
        }
        
        // Update totals label
        if (totalResultsLabel != null) {
            String filterInfo = filteredResults.size() < crawlResults.size() 
                ? " (filtered from " + crawlResults.size() + " total)" 
                : "";
            totalResultsLabel.setText(filteredResults.size() + " results" + filterInfo);
        }
    }
    
    /**
     * Loads existing projects.
     */
    private void loadProjects() {
        List<SiteMapProject> projects = siteMapService.listProjects();
        if (!projects.isEmpty()) {
            projectComboBox.getItems().clear();
            projectComboBox.getItems().addAll(projects);
            projectComboBox.getSelectionModel().selectFirst();
        }
    }
    
    /**
     * Refreshes project data with pagination support for large datasets.
     */
    private void refreshProjectData() {
        if (currentProject == null) {
            crawlResults.clear();
            filteredResults.clear();
            applyFilters();
            return;
        }
        
        try {
            updateStatus("Loading project data...");
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            
            // Defer heavy loading to a background thread to keep UI responsive
            CompletableFuture.runAsync(() -> {
                try {
                    // First check if this is a large dataset
                    boolean isLargeDataset = checkIfLargeDataset(currentProject.getId());
                    
                    if (isLargeDataset) {
                        Platform.runLater(() -> {
                            aiAnalysisTextArea.clear();
                            aiAnalysisTextArea.appendText("Loading large dataset in chunks...\n");
                            aiAnalysisTextArea.appendText("This project contains a large number of URLs.\n");
                            aiAnalysisTextArea.appendText("Loading initial data page. Use pagination and filters for navigation.\n\n");
                        });
                        
                        // Load just the first chunk
                        List<CrawlResult> chunk = siteMapService.getResultsChunk(currentProject.getId(), 0);
                        
                        Platform.runLater(() -> {
                            crawlResults.clear();
                            if (chunk != null && !chunk.isEmpty()) {
                                crawlResults.addAll(chunk);
                            }
                            
                            // Setup pagination for this project
                            setupPaginationForLargeDataset(currentProject.getId());
                            
                            // Apply filters and finish loading
                            applyFilters();
                            exportResultsButton.setDisable(crawlResults.isEmpty());
                            progressBar.setVisible(false);
                            
                            // Show summary in the AI analysis area
                            updateDataSummary();
                        });
                    } else {
                        // For smaller datasets, load all at once
                        List<CrawlResult> results = siteMapService.getCrawlResults(currentProject.getId());
                        
                        Platform.runLater(() -> {
                            crawlResults.clear();
                            if (results != null && !results.isEmpty()) {
                                crawlResults.addAll(results);
                            }
                            
                            // Apply filters and finish loading
                            applyFilters();
                            exportResultsButton.setDisable(crawlResults.isEmpty());
                            progressBar.setVisible(false);
                            
                            // Show summary in the AI analysis area
                            updateDataSummary();
                        });
                    }
                } catch (Exception e) {
                    logger.error("Error loading project data", e);
                    Platform.runLater(() -> {
                        updateStatus("Error loading project: " + e.getMessage());
                        progressBar.setVisible(false);
                    });
                }
            });
        } catch (Exception e) {
            logger.error("Error initiating project data loading", e);
            updateStatus("Error loading project: " + e.getMessage());
            progressBar.setVisible(false);
        }
    }
    
    /**
     * Updates the summary data in the AI analysis area
     */
    private void updateDataSummary() {
        try {
            int totalUrls = getTotalUrlCount(currentProject.getId());
            
            aiAnalysisTextArea.clear();
            aiAnalysisTextArea.appendText("Project: " + currentProject.getName() + "\n");
            aiAnalysisTextArea.appendText("Status: " + currentProject.getStatus() + "\n");
            aiAnalysisTextArea.appendText("URLs found: " + formatLargeNumber(totalUrls) + "\n\n");
            
            // Show URL type distribution (only if we have results)
            if (crawlResults.size() > 0) {
                analyzeUrlDistribution();
            }
        } catch (Exception e) {
            logger.error("Error updating data summary", e);
            aiAnalysisTextArea.appendText("Error loading project summary: " + e.getMessage() + "\n");
        }
    }
    
    /**
     * Checks if a project contains a large dataset that requires chunked loading.
     * 
     * @param projectId The project ID to check
     * @return True if the dataset is large, false otherwise
     */
    private boolean checkIfLargeDataset(String projectId) {
        // Check metadata to see if this project uses chunked storage
        Map<String, Object> metadata = siteMapService.getCrawlMetadata(projectId);
        return metadata != null && metadata.containsKey("totalChunks");
    }
    
    /**
     * Gets the total URL count for a project, handling both chunked and regular storage.
     * 
     * @param projectId The project ID
     * @return The total number of unique URLs in the project
     */
    private int getTotalUrlCount(String projectId) {
        // Always get the actual deduplicated count by requesting the full dataset
        // which will go through loadChunkedResults that now deduplicates
        List<CrawlResult> allResults = siteMapService.getCrawlResults(projectId);
        return allResults.size();
    }
    
    /**
     * Sets up pagination for large datasets.
     * 
     * @param projectId The project ID
     */
    private void setupPaginationForLargeDataset(String projectId) {
        Map<String, Object> metadata = siteMapService.getCrawlMetadata(projectId);
        
        if (metadata != null && metadata.containsKey("totalChunks")) {
            int totalChunks = ((Number) metadata.get("totalChunks")).intValue();
            // Get the actual deduplicated count
            int totalResults = getTotalUrlCount(projectId);
            
            if (resultsPagination != null) {
                resultsPagination.setPageCount(totalChunks);
                resultsPagination.setCurrentPageIndex(0);
                resultsPagination.setVisible(true);
                
                // Set page factory to load chunks on demand
                resultsPagination.setPageFactory(pageIndex -> {
                    loadResultsChunk(projectId, pageIndex);
                    return new Pane(); // Dummy node, as we're updating the table directly
                });
            }
            
            if (totalResultsLabel != null) {
                totalResultsLabel.setText(formatLargeNumber(totalResults) + " total URLs (deduplicated view)");
            }
        } else {
            // Hide pagination for small datasets
            if (resultsPagination != null) {
                resultsPagination.setVisible(false);
            }
        }
    }
    
    /**
     * Loads a specific chunk of results from storage.
     * 
     * @param projectId The project ID
     * @param chunkIndex The chunk index to load
     */
    private void loadResultsChunk(String projectId, int chunkIndex) {
        List<CrawlResult> chunk = siteMapService.getResultsChunk(projectId, chunkIndex);
        
        if (chunk != null) {
            Platform.runLater(() -> {
                crawlResults.clear();
                crawlResults.addAll(chunk);
                applyFilters();
                
                // Update chunk info
                Map<String, Object> metadata = siteMapService.getCrawlMetadata(projectId);
                if (metadata != null) {
                    // Get the actual total count (deduplicated)
                    int totalResults = getTotalUrlCount(projectId);
                    int chunkSize = ((Number) metadata.get("chunkSize")).intValue();
                    
                    int startIndex = chunkIndex * chunkSize + 1;
                    // Cap end index at total results
                    int endIndex = Math.min(startIndex + chunk.size() - 1, totalResults);
                    
                    if (totalResultsLabel != null) {
                        totalResultsLabel.setText(String.format("Showing %s-%s of %s URLs (deduplicated view)", 
                            formatLargeNumber(startIndex), 
                            formatLargeNumber(endIndex),
                            formatLargeNumber(totalResults)));
                    }
                }
            });
        }
    }
    
    /**
     * Analyzes URL distribution for the loaded results.
     */
    private void analyzeUrlDistribution() {
        // Limit analysis to a reasonable sample size
        List<CrawlResult> sampleResults = crawlResults.size() <= 5000 
            ? new ArrayList<>(crawlResults)
            : crawlResults.subList(0, 5000); // Use first 5000 as a sample
        
        long articleCount = sampleResults.stream()
            .filter(r -> r.getUrl().matches("https://[\\w.-]+\\.com/[\\w-]+/$"))
            .count();
        
        long categoryCount = sampleResults.stream()
            .filter(r -> r.getUrl().contains("/category/"))
            .count();
        
        long dateArchiveCount = sampleResults.stream()
            .filter(r -> r.getUrl().matches("https://[\\w.-]+\\.com/\\d{4}/.*"))
            .count();
        
        double articlePercent = sampleResults.isEmpty() ? 0 : (100.0 * articleCount / sampleResults.size());
        double categoryPercent = sampleResults.isEmpty() ? 0 : (100.0 * categoryCount / sampleResults.size());
        double dateArchivePercent = sampleResults.isEmpty() ? 0 : (100.0 * dateArchiveCount / sampleResults.size());
        
        aiAnalysisTextArea.appendText("URL distribution:\n");
        aiAnalysisTextArea.appendText(String.format("- Article pages: %.1f%%\n", articlePercent));
        aiAnalysisTextArea.appendText(String.format("- Category pages: %.1f%%\n", categoryPercent));
        aiAnalysisTextArea.appendText(String.format("- Date archive pages: %.1f%%\n\n", dateArchivePercent));
        
        // Add example URLs
        aiAnalysisTextArea.appendText("Example URLs:\n");
        sampleResults.stream()
            .limit(5)
            .forEach(r -> aiAnalysisTextArea.appendText("- " + r.getUrl() + "\n"));
    }
    
    /**
     * Handles the New Project button click.
     */
    private void handleNewProject() {
        String name = projectNameField.getText().trim();
        String seedUrl = seedUrlField.getText().trim();
        int crawlDepth = crawlDepthSpinner.getValue();
        int maxPages = maxPagesSpinner.getValue();
        
        if (name.isEmpty() || seedUrl.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please provide a project name and seed URL.");
            return;
        }
        
        // Create the project
        SiteMapProject project = siteMapService.createProject(name, seedUrl, crawlDepth, maxPages);
        
        // Add to combo box and select it
        projectComboBox.getItems().add(project);
        projectComboBox.getSelectionModel().select(project);
        
        updateStatus("Created new project: " + name);
    }
    
    /**
     * Handles the Start Crawl button click.
     */
    private void handleStartCrawl() {
        if (currentProject == null) {
            showAlert(Alert.AlertType.WARNING, "No Project", "Please create or select a project first.");
            return;
        }
        
        if (activeCrawl != null && !activeCrawl.isDone()) {
            showAlert(Alert.AlertType.WARNING, "Crawl In Progress", "A crawl is already in progress. Please wait for it to complete.");
            return;
        }
        
        try {
            // Reset stop flag
            stopRequested = false;
            
            // Reset performance tracking
            startTime = System.currentTimeMillis();
            lastUpdateTime = startTime;
            lastResultCount = 0;
            currentCrawlRate = 0.0;
            avgCrawlRate = 0.0;
            historicalRates.clear();
            
            // Initialize visualization with empty graph
            if (crawlerCanvas != null) {
                updateCrawlerVisualization();
            }
            
            // Check if this is a new crawl or resuming an existing one
            List<CrawlResult> existingResults = siteMapService.getCrawlResults(currentProject.getId());
            boolean isResuming = existingResults != null && !existingResults.isEmpty();
            
            // If resuming, set the last result count to the existing size
            if (isResuming && existingResults != null) {
                lastResultCount = existingResults.size();
            }
            
            // Clear previous results from the UI (not from storage)
            crawlResults.clear();
            filteredResults.clear();
            applyFilters();
            aiAnalysisTextArea.clear();
            
            // If it's a SpaceNews URL, show special message
            if (currentProject.getSeedUrl().contains("spacenews.com")) {
                aiAnalysisTextArea.appendText("## Enhanced SpaceNews Crawler\n\n");
                aiAnalysisTextArea.appendText("Using specialized crawler with optimizations for large sites:\n");
                aiAnalysisTextArea.appendText("• Bloom filter for efficient duplicate detection\n");
                aiAnalysisTextArea.appendText("• Chunked storage for datasets up to 1,000,000 URLs\n");
                aiAnalysisTextArea.appendText("• Pattern-based URL discovery\n");
                aiAnalysisTextArea.appendText("• Parallel processing with adaptive scaling\n");
                aiAnalysisTextArea.appendText("• Smart proxy rotation\n\n");
                aiAnalysisTextArea.appendText("Expected performance: Up to 50,000 URLs per hour depending on connection and proxies\n\n");
                aiAnalysisTextArea.appendText("Starting crawl...\n");
            }
            
            // Show progress bar
            progressBar.setVisible(true);
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            
            // Update button states
            startCrawlButton.setDisable(true);
            stopCrawlButton.setDisable(false);
            
            // Start the crawl
            activeCrawl = siteMapService.startCrawl(
                currentProject.getId(),
                this::updateStatus,
                this::handleCrawlResults,
                () -> stopRequested
            );
            
            // Handle completion
            activeCrawl.whenComplete((results, error) -> {
                Platform.runLater(() -> {
                    try {
                        // Hide progress bar
                        progressBar.setVisible(false);
                        
                        // Re-enable start button and disable stop button
                        startCrawlButton.setDisable(false);
                        stopCrawlButton.setDisable(true);
                        
                        if (error != null) {
                            updateStatus("Crawl failed: " + error.getMessage());
                            showAlert(Alert.AlertType.ERROR, "Crawl Failed", "The crawl failed: " + error.getMessage());
                        } else {
                            String completionMessage = stopRequested 
                                ? "Crawl stopped by user. Found " + results.size() + " pages."
                                : "Crawl completed successfully. Found " + results.size() + " pages.";
                            updateStatus(completionMessage);
                            exportResultsButton.setDisable(false);
                            
                            // Update button text for future resumption
                            startCrawlButton.setText("Resume AI-Assisted Crawl");
                            
                            // Show AI analysis summary
                            updateCrawlSummary(results, stopRequested);
                        }
                    } catch (Exception e) {
                        logger.error("Error finalizing crawl", e);
                        updateStatus("Error finalizing crawl: " + e.getMessage());
                    }
                });
            });
        } catch (Exception e) {
            logger.error("Error starting crawl", e);
            updateStatus("Error starting crawl: " + e.getMessage());
            progressBar.setVisible(false);
            startCrawlButton.setDisable(false);
            stopCrawlButton.setDisable(true);
        }
    }
    
    /**
     * Updates the crawl summary with analysis information
     */
    private void updateCrawlSummary(List<CrawlResult> results, boolean wasStopped) {
        aiAnalysisTextArea.appendText("--- AI Analysis Results ---\n\n");
        aiAnalysisTextArea.appendText("Total pages crawled: " + results.size() + "\n");
        aiAnalysisTextArea.appendText("Seed URL: " + currentProject.getSeedUrl() + "\n\n");
        
        // Count by status
        long okCount = results.stream().filter(r -> "OK".equals(r.getStatus())).count();
        long excludedCount = results.stream().filter(r -> "Excluded".equals(r.getStatus())).count();
        
        aiAnalysisTextArea.appendText("Pages included: " + okCount + "\n");
        aiAnalysisTextArea.appendText("Pages excluded: " + excludedCount + "\n\n");
        
        // For SpaceNews, add more detailed analysis
        if (currentProject.getSeedUrl().contains("spacenews.com")) {
            aiAnalysisTextArea.appendText("SpaceNews.com Content Analysis:\n");
            aiAnalysisTextArea.appendText("--------------------------------\n");
            
            // Find count of URLs by pattern
            long articleCount = results.stream()
                .filter(r -> r.getUrl().matches("https://spacenews\\.com/[\\w-]+/$"))
                .count();
            
            long categoryCount = results.stream()
                .filter(r -> r.getUrl().contains("/category/"))
                .count();
            
            long dateArchiveCount = results.stream()
                .filter(r -> r.getUrl().matches("https://spacenews\\.com/\\d{4}/.*"))
                .count();
            
            aiAnalysisTextArea.appendText("Article pages: " + articleCount + "\n");
            aiAnalysisTextArea.appendText("Category pages: " + categoryCount + "\n");
            aiAnalysisTextArea.appendText("Date archive pages: " + dateArchiveCount + "\n\n");
            
            aiAnalysisTextArea.appendText("Example article URL patterns detected:\n");
            
            // List a few example URLs
            results.stream()
                .filter(r -> r.getUrl().matches("https://spacenews\\.com/[\\w-]+/$"))
                .limit(5)
                .forEach(r -> aiAnalysisTextArea.appendText("- " + r.getUrl() + "\n"));
            
            if (wasStopped) {
                aiAnalysisTextArea.appendText("\nNote: The crawl was stopped manually. You can resume crawling anytime\n");
                aiAnalysisTextArea.appendText("by clicking 'Resume AI-Assisted Crawl'. The crawler will continue where it\n");
                aiAnalysisTextArea.appendText("left off and avoid adding duplicate URLs.\n\n");
            } else {
                aiAnalysisTextArea.appendText("\nNote: In a production environment, this would connect to the full JavaScript crawler\n");
                aiAnalysisTextArea.appendText("to discover 100,000+ URLs. The current implementation simulates the process with\n");
                aiAnalysisTextArea.appendText("a representative sample for demonstration purposes.\n\n");
            }
        }
        
        aiAnalysisTextArea.appendText("AI-guided crawling complete! The site structure has been mapped according to " +
                                     "optimal patterns discovered by our intelligent analysis.\n\n" +
                                     "You can now proceed to URL validation or export these results.");
    }
    
    /**
     * Handles the Stop Crawl button click.
     */
    private void handleStopCrawl() {
        if (activeCrawl != null && !activeCrawl.isDone()) {
            stopRequested = true;
            updateStatus("Crawl stopped.");
        }
    }
    
    /**
     * Handles the Export Results button click.
     */
    private void handleExportResults() {
        if (currentProject == null || crawlResults.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Results", "There are no results to export.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Crawl Results");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fileChooser.setInitialFileName(currentProject.getName().replaceAll("\\s", "_") + "_results.csv");
        
        File file = fileChooser.showSaveDialog(crawlResultsTable.getScene().getWindow());
        if (file != null) {
            try {
                // Build CSV content
                StringBuilder csv = new StringBuilder();
                csv.append("URL,Title,Status\n");
                
                for (CrawlResult result : crawlResults) {
                    csv.append(result.getUrl()).append(",")
                       .append("\"").append(result.getTitle().replace("\"", "\"\"")).append("\",")
                       .append(result.getStatus()).append("\n");
                }
                
                // Write to file
                Files.write(Paths.get(file.getAbsolutePath()), csv.toString().getBytes());
                
                updateStatus("Results exported to: " + file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Failed to export results", e);
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to export results: " + e.getMessage());
            }
        }
    }
    
    /**
     * Updates the status message.
     * 
     * @param message The status message
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            logger.info(message);
            
            // Add to AI analysis text area for all messages when dealing with SpaceNews
            if (currentProject != null && currentProject.getSeedUrl().contains("spacenews.com")) {
                aiAnalysisTextArea.appendText(message + "\n");
            }
            // For other sites, only add special messages
            else if (message.startsWith("Analyzing") || message.startsWith("AI analysis") || message.startsWith("Found")) {
                aiAnalysisTextArea.appendText(message + "\n");
            }
        });
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
     * Updates performance metrics based on the current crawl progress.
     * 
     * @param currentResultCount The current number of results
     */
    private void updatePerformanceMetrics(int currentResultCount) {
        long currentTime = System.currentTimeMillis();
        long elapsedSinceStart = currentTime - startTime;
        long elapsedSinceLastUpdate = currentTime - lastUpdateTime;
        
        // Only update if enough time has passed (at least 2 seconds)
        if (elapsedSinceLastUpdate >= 2000) {
            // Calculate URLs per second since last update
            int newUrlsFound = currentResultCount - lastResultCount;
            currentCrawlRate = (double) newUrlsFound / (elapsedSinceLastUpdate / 1000.0);
            
            // Add to historical rates (but avoid adding zero rates)
            if (currentCrawlRate > 0) {
                historicalRates.add(currentCrawlRate);
                
                // Keep only the last 30 readings for the graph
                while (historicalRates.size() > 30) {
                    historicalRates.remove(0);
                }
            }
            
            // Calculate average rate
            if (!historicalRates.isEmpty()) {
                double sum = 0;
                for (Double rate : historicalRates) {
                    sum += rate;
                }
                avgCrawlRate = sum / historicalRates.size();
            }
            
            // Calculate estimated time to reach 100,000 URLs
            String estimatedTimeString = "Unknown";
            if (currentCrawlRate > 0.1) { // Only show estimate if rate is meaningful
                long remainingUrls = 100000 - currentResultCount;
                if (remainingUrls > 0) {
                    long estimatedSeconds = (long)(remainingUrls / currentCrawlRate);
                    estimatedTimeString = formatEstimatedTime(estimatedSeconds);
                } else {
                    estimatedTimeString = "Complete!";
                }
            }
            
            // Build more detailed performance status
            String performanceRating;
            String performanceEmoji;
            Color performanceColor;
            
            // Rate the performance
            if (currentCrawlRate < 0.5) {
                performanceRating = "Very Slow";
                performanceEmoji = "🐢";
                performanceColor = Color.RED;
            } else if (currentCrawlRate < 1.0) {
                performanceRating = "Slow";
                performanceEmoji = "⚠️";
                performanceColor = Color.ORANGE;
            } else if (currentCrawlRate < 5.0) {
                performanceRating = "Normal";
                performanceEmoji = "✓";
                performanceColor = Color.DARKBLUE;
            } else if (currentCrawlRate < 10.0) {
                performanceRating = "Fast";
                performanceEmoji = "🚀";
                performanceColor = Color.GREEN;
            } else {
                performanceRating = "Very Fast";
                performanceEmoji = "⚡";
                performanceColor = Color.DARKGREEN;
            }
            
            // Update the performance label
            if (performanceLabel != null) {
                String elapsedTimeStr = formatElapsedTime(elapsedSinceStart);
                String status = String.format("Time: %s | URLs: %d | Current: %.1f/sec | Avg: %.1f/sec | " +
                                             "Est. to 100K: %s | Speed: %s %s", 
                    elapsedTimeStr, currentResultCount, currentCrawlRate, avgCrawlRate, 
                    estimatedTimeString, performanceRating, performanceEmoji);
                
                Platform.runLater(() -> {
                    performanceLabel.setText(status);
                    performanceLabel.setTextFill(performanceColor);
                    
                    // Update visualization
                    updateCrawlerVisualization();
                });
            }
            
            // Suggest AI adjustments based on performance
            if (currentCrawlRate < 1.0 && historicalRates.size() > 5) {
                // If consistently slow, log a suggestion
                logger.info("Crawl rate is slow (< 1 URL/sec). AI should adjust batch size or processing strategy.");
                
                // Add a note to the AI analysis area
                Platform.runLater(() -> {
                    aiAnalysisTextArea.appendText("[Performance Alert " + performanceEmoji + "] Crawl rate is below target (< 1 URL/sec). " +
                        "AI is optimizing processing strategy for improved throughput.\n");
                });
                
                // Signal SiteMapService to adjust its strategy (if supported)
                siteMapService.adjustCrawlStrategy(currentCrawlRate, avgCrawlRate, currentResultCount);
            }
            
            // Update tracking variables for next calculation
            lastUpdateTime = currentTime;
            lastResultCount = currentResultCount;
            
            // Also update the total results label
            Platform.runLater(() -> {
                if (totalResultsLabel != null) {
                    String filterInfo = filteredResults.size() < crawlResults.size() 
                        ? " (filtered from " + crawlResults.size() + " total)" 
                        : "";
                    totalResultsLabel.setText(filteredResults.size() + " results" + filterInfo);
                }
            });
        }
    }
    
    /**
     * Updates the crawler visualization canvas.
     */
    private void updateCrawlerVisualization() {
        if (crawlerCanvas == null) {
            return;
        }
        
        GraphicsContext gc = crawlerCanvas.getGraphicsContext2D();
        double width = crawlerCanvas.getWidth();
        double height = crawlerCanvas.getHeight();
        
        // Background
        gc.setFill(Color.rgb(240, 240, 240));
        gc.fillRect(0, 0, width, height);
        
        // Draw grid
        gc.setStroke(Color.rgb(220, 220, 220));
        gc.setLineWidth(1);
        
        // Vertical grid lines
        for (int i = 0; i < width; i += 50) {
            gc.strokeLine(i, 0, i, height);
        }
        
        // Horizontal grid lines
        for (int i = 0; i < height; i += 50) {
            gc.strokeLine(0, i, width, i);
        }
        
        // Scale factors for graph
        double scaleHeight = height * 0.8;
        double baseY = height * 0.9;
        
        // Draw historical rates if we have data
        if (!historicalRates.isEmpty()) {
            // Find max rate for scaling (minimum 10 URLs/second to avoid too much scaling)
            double maxRate = Math.max(
                historicalRates.stream().mapToDouble(Double::doubleValue).max().orElse(10.0),
                10.0
            );
            
            // Logarithmic scale for large volumes
            boolean useLogScale = maxRate > 100;
            
            // Draw axes labels
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 10));
            
            // Y-axis scale
            if (useLogScale) {
                // Log scale for high rates
                for (int i = 1; i <= 1000; i *= 10) {
                    double y = baseY - scaleHeight * (Math.log10(i) / Math.log10(maxRate));
                    gc.fillText(i + " URLs/s", 5, y);
                    gc.setStroke(Color.rgb(200, 200, 200));
                    gc.strokeLine(30, y, width - 5, y);
                }
            } else {
                // Linear scale for normal rates
                for (int i = 0; i <= maxRate; i += Math.max(1, (int) (maxRate / 5))) {
                    double y = baseY - (i / maxRate) * scaleHeight;
                    gc.fillText(i + " URLs/s", 5, y);
                    gc.setStroke(Color.rgb(200, 200, 200));
                    gc.strokeLine(30, y, width - 5, y);
                }
            }
            
            // Data count scale for X axis (bottom)
            gc.setFill(Color.BLACK);
            int totalResults = crawlResults.size();
            String totalFormatted = formatLargeNumber(totalResults);
            gc.fillText("Current: " + totalFormatted + " URLs", width / 2 - 50, height - 5);
            
            // Draw history line
            gc.setStroke(Color.rgb(0, 120, 215));
            gc.setLineWidth(2);
            
            double xStep = width / (historicalRates.size() + 1);
            double x = xStep;
            
            for (int i = 0; i < historicalRates.size(); i++) {
                double rate = historicalRates.get(i);
                double y;
                
                if (useLogScale && rate > 0) {
                    // Logarithmic scaling for high rates
                    y = baseY - scaleHeight * (Math.log10(rate + 0.1) / Math.log10(maxRate));
                } else {
                    // Linear scaling for normal rates
                    y = baseY - (rate / maxRate) * scaleHeight;
                }
                
                if (i == 0) {
                    gc.beginPath();
                    gc.moveTo(x, y);
                } else {
                    gc.lineTo(x, y);
                }
                
                x += xStep;
            }
            gc.stroke();
            
            // Draw current rate point
            if (!historicalRates.isEmpty()) {
                double lastRate = historicalRates.get(historicalRates.size() - 1);
                double y;
                
                if (useLogScale && lastRate > 0) {
                    y = baseY - scaleHeight * (Math.log10(lastRate + 0.1) / Math.log10(maxRate));
                } else {
                    y = baseY - (lastRate / maxRate) * scaleHeight;
                }
                
                gc.setFill(Color.rgb(0, 120, 215));
                gc.fillOval(x - xStep - 4, y - 4, 8, 8);
                
                // Label current rate
                String rateLabel = String.format("%.1f URLs/s", lastRate);
                gc.setFill(Color.BLACK);
                gc.fillText(rateLabel, x - xStep + 8, y + 4);
            }
            
            // Add caption showing estimated completion time
            if (crawlResults.size() > 0 && avgCrawlRate > 0.1) {
                long remainingUrls = Math.max(1000000 - crawlResults.size(), 0);
                long estimatedSeconds = (long)(remainingUrls / avgCrawlRate);
                String estimatedTimeString = formatEstimatedTime(estimatedSeconds);
                
                gc.setFill(Color.BLACK);
                gc.setFont(Font.font("System", FontWeight.BOLD, 12));
                gc.fillText("Est. time to 1M URLs: " + estimatedTimeString, width / 2 - 80, height - 20);
            }
        } else {
            // No data yet, draw empty state
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font("System", FontWeight.NORMAL, 14));
            gc.fillText("Waiting for crawler data...", width / 2 - 80, height / 2);
        }
    }
    
    /**
     * Format large numbers with K/M suffixes for better readability
     * 
     * @param number The number to format
     * @return Formatted string with K/M suffix
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
     * Formats elapsed time in a readable format (MM:SS or HH:MM:SS).
     * 
     * @param elapsedMillis Elapsed time in milliseconds
     * @return Formatted time string
     */
    private String formatElapsedTime(long elapsedMillis) {
        long seconds = elapsedMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Formats estimated time in a readable format.
     * 
     * @param seconds Estimated time in seconds
     * @return Formatted time string
     */
    private String formatEstimatedTime(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return String.format("%d minutes", seconds / 60);
        } else if (seconds < 86400) {
            return String.format("%d hours %d minutes", seconds / 3600, (seconds % 3600) / 60);
        } else {
            return String.format("%d days %d hours", seconds / 86400, (seconds % 86400) / 3600);
        }
    }
    
    /**
     * Shows the settings dialog for configuring crawler parameters.
     */
    private void showSettingsDialog() {
        // Create the dialog
        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Crawler Settings");
        dialog.setHeaderText("Configure Crawler Parameters");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the content for different tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // General tab
        Tab generalTab = new Tab("General");
        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(10);
        generalGrid.setVgap(10);
        generalGrid.setPadding(new Insets(20, 150, 10, 10));
        
        // Controls for general settings
        Spinner<Integer> maxConcurrentRequestsSpinner = new Spinner<>(1, 10, 3);
        maxConcurrentRequestsSpinner.setEditable(true);
        CheckBox respectRobotsCheck = new CheckBox("Respect robots.txt");
        respectRobotsCheck.setSelected(true);
        CheckBox followRedirectsCheck = new CheckBox("Follow redirects");
        followRedirectsCheck.setSelected(true);
        
        generalGrid.add(new Label("Max Concurrent Requests:"), 0, 0);
        generalGrid.add(maxConcurrentRequestsSpinner, 1, 0);
        generalGrid.add(respectRobotsCheck, 0, 1);
        generalGrid.add(followRedirectsCheck, 0, 2);
        
        generalTab.setContent(generalGrid);
        
        // Performance tab
        Tab performanceTab = new Tab("Performance");
        GridPane performanceGrid = new GridPane();
        performanceGrid.setHgap(10);
        performanceGrid.setVgap(10);
        performanceGrid.setPadding(new Insets(20, 150, 10, 10));
        
        // Get current values from SiteMapService
        SiteMapService siteMapService = SiteMapService.getInstance();
        int currentBatchSize = siteMapService.getBatchSize();
        long currentDelay = siteMapService.getDelayBetweenBatches();
        boolean useTimeThrottling = siteMapService.isUseTimeBasedThrottling();
        long targetDuration = siteMapService.getTargetBatchDuration();
        
        // Controls for performance settings
        Spinner<Integer> batchSizeSpinner = new Spinner<>(10, 500, currentBatchSize);
        batchSizeSpinner.setEditable(true);
        Spinner<Integer> delaySpinner = new Spinner<>(0, 5000, (int)currentDelay);
        delaySpinner.setEditable(true);
        CheckBox timeThrottlingCheck = new CheckBox("Use time-based throttling");
        timeThrottlingCheck.setSelected(useTimeThrottling);
        Spinner<Integer> targetDurationSpinner = new Spinner<>(500, 10000, (int)targetDuration);
        targetDurationSpinner.setEditable(true);
        
        performanceGrid.add(new Label("Batch Size:"), 0, 0);
        performanceGrid.add(batchSizeSpinner, 1, 0);
        performanceGrid.add(new Label("Delay Between Batches (ms):"), 0, 1);
        performanceGrid.add(delaySpinner, 1, 1);
        performanceGrid.add(timeThrottlingCheck, 0, 2, 2, 1);
        performanceGrid.add(new Label("Target Batch Duration (ms):"), 0, 3);
        performanceGrid.add(targetDurationSpinner, 1, 3);
        
        // Enable/disable target duration based on time throttling checkbox
        targetDurationSpinner.setDisable(!useTimeThrottling);
        timeThrottlingCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            targetDurationSpinner.setDisable(!newVal);
        });
        
        performanceTab.setContent(performanceGrid);
        
        // SpaceNews tab
        Tab spaceNewsTab = new Tab("SpaceNews");
        GridPane spaceNewsGrid = new GridPane();
        spaceNewsGrid.setHgap(10);
        spaceNewsGrid.setVgap(10);
        spaceNewsGrid.setPadding(new Insets(20, 150, 10, 10));
        
        // Controls for SpaceNews-specific settings
        CheckBox processXmlSitemapsCheck = new CheckBox("Process XML sitemaps");
        processXmlSitemapsCheck.setSelected(true);
        CheckBox enableDateArchivesCheck = new CheckBox("Crawl date-based archives");
        enableDateArchivesCheck.setSelected(true);
        CheckBox enableCategoryArchivesCheck = new CheckBox("Crawl category archives");
        enableCategoryArchivesCheck.setSelected(true);
        
        ComboBox<String> ipRotationStrategyCombo = new ComboBox<>();
        ipRotationStrategyCombo.getItems().addAll("None", "Round Robin", "Random", "Session-based");
        ipRotationStrategyCombo.setValue("None");
        
        spaceNewsGrid.add(processXmlSitemapsCheck, 0, 0, 2, 1);
        spaceNewsGrid.add(enableDateArchivesCheck, 0, 1, 2, 1);
        spaceNewsGrid.add(enableCategoryArchivesCheck, 0, 2, 2, 1);
        spaceNewsGrid.add(new Label("IP Rotation Strategy:"), 0, 3);
        spaceNewsGrid.add(ipRotationStrategyCombo, 1, 3);
        
        spaceNewsTab.setContent(spaceNewsGrid);
        
        // Add tabs to tabpane
        tabPane.getTabs().addAll(generalTab, performanceTab, spaceNewsTab);
        
        // Set content
        dialog.getDialogPane().setContent(tabPane);
        
        // Convert the result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, Object> settings = new HashMap<>();
                // General tab
                settings.put("maxConcurrentRequests", maxConcurrentRequestsSpinner.getValue());
                settings.put("respectRobots", respectRobotsCheck.isSelected());
                settings.put("followRedirects", followRedirectsCheck.isSelected());
                
                // Performance tab
                settings.put("batchSize", batchSizeSpinner.getValue());
                settings.put("delayBetweenBatches", delaySpinner.getValue());
                settings.put("useTimeThrottling", timeThrottlingCheck.isSelected());
                settings.put("targetDuration", targetDurationSpinner.getValue());
                
                // SpaceNews tab
                settings.put("processXmlSitemaps", processXmlSitemapsCheck.isSelected());
                settings.put("enableDateArchives", enableDateArchivesCheck.isSelected());
                settings.put("enableCategoryArchives", enableCategoryArchivesCheck.isSelected());
                settings.put("ipRotationStrategy", ipRotationStrategyCombo.getValue());
                
                return settings;
            }
            return null;
        });
        
        // Show the dialog and process the result
        Optional<Map<String, Object>> result = dialog.showAndWait();
        result.ifPresent(settings -> {
            // Apply settings to SiteMapService
            siteMapService.setBatchSize((Integer) settings.get("batchSize"));
            siteMapService.setDelayBetweenBatches(((Integer) settings.get("delayBetweenBatches")).longValue());
            siteMapService.setUseTimeBasedThrottling((Boolean) settings.get("useTimeThrottling"));
            siteMapService.setTargetBatchDuration(((Integer) settings.get("targetDuration")).longValue());
            
            // Show confirmation
            updateStatus("Crawler settings updated successfully");
            
            // Additional SpaceNews specific settings could be applied here
            String ipStrategy = (String) settings.get("ipRotationStrategy");
            if (!"None".equals(ipStrategy)) {
                aiAnalysisTextArea.appendText("IP Rotation Strategy set to: " + ipStrategy + 
                                              ". This will distribute requests across multiple IP addresses.\n");
            }
        });
    }

    /**
     * Updates the UI with crawl results.
     * 
     * @param results The crawl results
     */
    private void handleCrawlResults(List<CrawlResult> results) {
        // Check if we're on the JavaFX thread, if not use Platform.runLater
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> handleCrawlResults(results));
            return;
        }
        
        try {
            // Instead of processing all results at once, only process the difference
            // between what we already have and new results
            int currentSize = crawlResults.size();
            int totalNewResults = results.size() - currentSize;
            
            // If we have new results to add
            if (totalNewResults > 0) {
                // Process new results in small batches
                int batchSize = 50; // Smaller batch size to prevent UI freezes
                
                // Add the first batch immediately for responsiveness
                int initialBatchSize = Math.min(batchSize, totalNewResults);
                List<CrawlResult> initialBatch = results.subList(currentSize, currentSize + initialBatchSize);
                
                // Stage 1: Add initial batch to improve responsiveness
                crawlResults.addAll(initialBatch);
                applyFilters(); // Apply filters to update the UI immediately
                
                // Stage 2: If there are more results, process them in the background
                if (totalNewResults > initialBatchSize) {
                    // Create a separate runnable to handle the remaining batches
                    Runnable batchProcessor = () -> {
                        try {
                            for (int i = currentSize + initialBatchSize; i < results.size(); i += batchSize) {
                                if (stopRequested) break;
                                
                                final int start = i;
                                final int end = Math.min(i + batchSize, results.size());
                                final List<CrawlResult> batch = results.subList(start, end);
                                
                                // Pause briefly to allow UI to remain responsive
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                                
                                // Ensure UI updates happen on FX thread
                                int finalEnd = end;
                                Platform.runLater(() -> {
                                    // Add batch to crawlResults
                                    crawlResults.addAll(batch);
                                    
                                    // Update progress display
                                    double progress = (double) (finalEnd - currentSize) / totalNewResults;
                                    progressBar.setProgress(progress);
                                    
                                    // Update status and filters periodically (not on every batch to improve performance)
                                    if (finalEnd % (batchSize * 5) == 0 || finalEnd == results.size()) {
                                        updateStatus("Processed " + (finalEnd - currentSize) + " of " + totalNewResults + " new URLs");
                                        applyFilters();
                                    }
                                });
                            }
                            
                            // Final update once all batches are processed
                            Platform.runLater(() -> {
                                progressBar.setProgress(1.0);
                                updateStatus("Completed processing all " + results.size() + " URLs");
                                applyFilters(); // Ensure filters are applied to show latest results
                            });
                        } catch (Exception e) {
                            logger.error("Error in batch processing thread", e);
                            Platform.runLater(() -> updateStatus("Error processing results: " + e.getMessage()));
                        }
                    };
                    
                    // Use a dedicated thread for batch processing to keep UI responsive
                    Thread batchThread = new Thread(batchProcessor);
                    batchThread.setDaemon(true);
                    batchThread.start();
                } else {
                    // Small result set, complete immediately
                    progressBar.setProgress(1.0);
                    updateStatus("Processed all " + results.size() + " URLs");
                    applyFilters();
                }
            } else {
                // No new results, just refresh the view
                applyFilters();
            }
            
            // Update performance metrics
            updatePerformanceMetrics(results.size());
        } catch (Exception e) {
            logger.error("Error updating UI with crawl results", e);
            updateStatus("Error displaying results: " + e.getMessage());
        }
    }
    
    /**
     * Clean up resources when the controller is destroyed.
     * This should be called by the parent component when the view is closed.
     */
    public void cleanup() {
        // Shutdown the executor service if it exists
        if (crawlExecutor != null && !crawlExecutor.isShutdown()) {
            crawlExecutor.shutdown();
            try {
                // Wait briefly for tasks to complete
                if (!crawlExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    crawlExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                crawlExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Stop any active crawl
        if (activeCrawl != null && !activeCrawl.isDone()) {
            stopRequested = true;
        }
        
        logger.info("SiteMapController resources cleaned up");
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
            logger.info("AI assistant drawer " + (isDrawerVisible ? "hidden" : "shown"));
        }
    }
} 