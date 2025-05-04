package com.spacedataarchive.controller;

import com.spacedataarchive.model.CatalogProcessingResult;
import com.spacedataarchive.model.ProcessingJob;
import com.spacedataarchive.service.CatalogProcessingService;
import com.spacedataarchive.service.ServiceFactory;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the catalog processing UI.
 */
public class CatalogProcessingController {
    
    @FXML private VBox processingContainer;
    @FXML private TextField jobNameField;
    @FXML private TextField seedUrlField;
    @FXML private TextField maxDepthField;
    @FXML private TextField maxUrlsField;
    @FXML private Button createJobButton;
    @FXML private Button startMappingButton;
    @FXML private Button startValidationButton;
    @FXML private Button startExtractionButton;
    @FXML private Button startEnhancementButton;
    @FXML private Button stopProcessingButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea logArea;
    @FXML private ComboBox<ProcessingJob> jobSelector;
    @FXML private TableView<Map.Entry<String, Object>> statisticsTable;
    @FXML private TableColumn<Map.Entry<String, Object>, String> statNameColumn;
    @FXML private TableColumn<Map.Entry<String, Object>, String> statValueColumn;
    
    // Process flow indicators
    @FXML private Circle phase1Circle;
    @FXML private Circle phase2Circle;
    @FXML private Circle phase3Circle;
    @FXML private Circle phase4Circle;
    @FXML private Circle phase5Circle;
    @FXML private Label phaseDetailLabel;
    
    // Process phase colors
    private final Color WAITING_COLOR = Color.rgb(224, 224, 224); // Light gray
    private final Color ACTIVE_COLOR = Color.rgb(33, 150, 243);   // Blue
    private final Color COMPLETED_COLOR = Color.rgb(76, 175, 80); // Green
    private final Color ERROR_COLOR = Color.rgb(244, 67, 54);     // Red
    
    private CatalogProcessingService catalogProcessingService;
    private ObservableList<ProcessingJob> jobs = FXCollections.observableArrayList();
    private ProcessingJob currentJob;
    private CompletableFuture<CatalogProcessingResult> activeProcessingTask;
    
    private java.util.Timer statusPollTimer;
    
    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        catalogProcessingService = ServiceFactory.getInstance().getCatalogProcessingService();
        
        // Configure the job selector
        jobSelector.setItems(jobs);
        jobSelector.setConverter(new javafx.util.StringConverter<ProcessingJob>() {
            @Override
            public String toString(ProcessingJob job) {
                return job == null ? "" : job.getName() + " (" + job.getId() + ")";
            }
            
            @Override
            public ProcessingJob fromString(String string) {
                return null; // Not needed for ComboBox
            }
        });
        
        jobSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentJob = newVal;
            updateUIForJobSelection();
        });
        
        // Configure the statistics table
        statNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        statValueColumn.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getValue())));
        
        // Initialize process flow indicators
        resetProcessPhases();
        
        // Load existing jobs
        refreshJobList();
        
        // Setup initial UI state
        updateUIForNoJob();
    }
    
    /**
     * Refreshes the list of jobs.
     */
    @FXML
    public void refreshJobList() {
        List<ProcessingJob> allJobs = catalogProcessingService.listJobs();
        jobs.clear();
        jobs.addAll(allJobs);
        
        if (!allJobs.isEmpty() && currentJob == null) {
            jobSelector.getSelectionModel().select(0);
        }
    }
    
    /**
     * Creates a new processing job.
     */
    @FXML
    public void createJob() {
        String name = jobNameField.getText().trim();
        String seedUrl = seedUrlField.getText().trim();
        
        if (name.isEmpty() || seedUrl.isEmpty()) {
            updateStatus("Job name and seed URL are required", true);
            return;
        }
        
        // Ensure URL has proper format with protocol
        if (!seedUrl.startsWith("http://") && !seedUrl.startsWith("https://")) {
            seedUrl = "https://" + seedUrl;
            appendLog("Added protocol to URL: " + seedUrl);
        }
        
        int maxDepth = 3; // Default
        int maxUrls = 10000; // Default
        
        try {
            if (!maxDepthField.getText().trim().isEmpty()) {
                maxDepth = Integer.parseInt(maxDepthField.getText().trim());
            }
            
            if (!maxUrlsField.getText().trim().isEmpty()) {
                maxUrls = Integer.parseInt(maxUrlsField.getText().trim());
            }
        } catch (NumberFormatException e) {
            updateStatus("Invalid number format", true);
            return;
        }
        
        ProcessingJob job = catalogProcessingService.createJob(name, seedUrl, maxDepth, maxUrls);
        jobs.add(job);
        jobSelector.getSelectionModel().select(job);
        updateStatus("Created new job: " + job.getName(), false);
        
        // Clear fields
        jobNameField.clear();
        seedUrlField.clear();
        maxDepthField.clear();
        maxUrlsField.clear();
    }
    
    /**
     * Starts the mapping phase for the current job.
     */
    @FXML
    public void startMapping() {
        if (currentJob == null) {
            updateStatus("No job selected", true);
            return;
        }
        
        startProcessingPhase(ProcessingPhase.MAPPING);
    }
    
    /**
     * Starts the validation phase for the current job.
     */
    @FXML
    public void startValidation() {
        if (currentJob == null) {
            updateStatus("No job selected", true);
            return;
        }
        
        startProcessingPhase(ProcessingPhase.VALIDATION);
    }
    
    /**
     * Starts the extraction phase for the current job.
     */
    @FXML
    public void startExtraction() {
        if (currentJob == null) {
            updateStatus("No job selected", true);
            return;
        }
        
        startProcessingPhase(ProcessingPhase.EXTRACTION);
    }
    
    /**
     * Starts the enhancement phase for the current job.
     */
    @FXML
    public void startEnhancement() {
        if (currentJob == null) {
            updateStatus("No job selected", true);
            return;
        }
        
        startProcessingPhase(ProcessingPhase.ENHANCEMENT);
    }
    
    /**
     * Stops the current processing job.
     */
    @FXML
    public void stopProcessing() {
        if (currentJob == null) {
            updateStatus("No job selected", true);
            return;
        }
        
        boolean stopped = catalogProcessingService.stopJob(currentJob.getId());
        if (stopped) {
            updateStatus("Requested to stop job: " + currentJob.getName(), false);
            appendLog("Job stop requested. This may take a moment to complete...");
        } else {
            updateStatus("Failed to stop job: " + currentJob.getName(), true);
        }
    }
    
    /**
     * Processing phases.
     */
    private enum ProcessingPhase {
        MAPPING,
        VALIDATION,
        EXTRACTION,
        ENHANCEMENT
    }
    
    /**
     * Starts a processing phase for the current job.
     *
     * @param phase The phase to start
     */
    private void startProcessingPhase(ProcessingPhase phase) {
        // Disable UI controls
        setProcessingUIState(true);
        clearLog();
        
        // Reset process flow indicators for a new mapping job
        if (phase == ProcessingPhase.MAPPING) {
            resetProcessPhases();
        }
        
        // Start the appropriate phase
        switch (phase) {
            case MAPPING:
                updateStatus("Starting AI-enhanced mapping process...", false);
                activeProcessingTask = catalogProcessingService.startMapping(
                    currentJob.getId(),
                    this::handleProcessingPhaseMessage,
                    this::handleResults
                );
                break;
                
            case VALIDATION:
                updateStatus("Starting validation process...", false);
                activeProcessingTask = catalogProcessingService.startValidation(
                    currentJob.getId(),
                    this::appendLog,
                    this::handleResults
                );
                break;
                
            case EXTRACTION:
                updateStatus("Starting extraction process...", false);
                activeProcessingTask = catalogProcessingService.startExtraction(
                    currentJob.getId(),
                    this::appendLog,
                    this::handleResults
                );
                break;
                
            case ENHANCEMENT:
                updateStatus("Starting enhancement process...", false);
                activeProcessingTask = catalogProcessingService.startEnhancement(
                    currentJob.getId(),
                    this::appendLog,
                    this::handleResults
                );
                break;
        }
        
        // Handle completion
        activeProcessingTask.whenComplete((result, ex) -> {
            Platform.runLater(() -> {
                if (ex != null) {
                    updateStatus("Processing failed: " + ex.getMessage(), true);
                    
                    // Mark the current phase as having an error
                    if (phase == ProcessingPhase.MAPPING) {
                        // Try to determine which sub-phase had the error
                        String errorMessage = ex.getMessage();
                        if (errorMessage.contains("AI Reconnaissance") || errorMessage.contains("analyze")) {
                            markPhaseError(1, ex.getMessage());
                        } else if (errorMessage.contains("setup") || errorMessage.contains("initialize")) {
                            markPhaseError(2, ex.getMessage());
                        } else if (errorMessage.contains("crawl") || errorMessage.contains("sitemap")) {
                            markPhaseError(3, ex.getMessage());
                        } else if (errorMessage.contains("validate") || errorMessage.contains("extract")) {
                            markPhaseError(4, ex.getMessage());
                        } else if (errorMessage.contains("result") || errorMessage.contains("process")) {
                            markPhaseError(5, ex.getMessage());
                        } else {
                            // Default to crawling phase if we can't determine
                            markPhaseError(3, ex.getMessage());
                        }
                    }
                } else {
                    updateStatus("Processing completed successfully", false);
                    
                    if (phase == ProcessingPhase.MAPPING) {
                        // Mark all phases as completed for a successful mapping
                        updateProcessPhase(6, "Mapping completed successfully! Found " + 
                            result.getTotalProcessedCount() + " URLs with " + 
                            result.getSuccessCount() + " valid URLs.");
                    }
                    
                    refreshJobData();
                }
                
                // Re-enable UI
                setProcessingUIState(false);
            });
        });
    }
    
    /**
     * Handles processing phase messages and updates the process flow UI.
     * 
     * @param message The message from the processing service
     */
    private void handleProcessingPhaseMessage(String message) {
        appendLog(message);
        
        // Extract phase information from the message
        if (message.startsWith("[Phase 1]")) {
            updateProcessPhase(1, message.substring(9).trim());
        } else if (message.startsWith("[Phase 2]")) {
            updateProcessPhase(2, message.substring(9).trim());
        } else if (message.startsWith("[Phase 3]")) {
            updateProcessPhase(3, message.substring(9).trim());
        } else if (message.startsWith("[Phase 4]")) {
            updateProcessPhase(4, message.substring(9).trim());
        } else if (message.startsWith("[Phase 5]")) {
            updateProcessPhase(5, message.substring(9).trim());
        }
        // Special case for errors
        else if (message.contains("ERROR") || message.contains("failed")) {
            // Try to determine which phase has the error
            for (int i = 1; i <= 5; i++) {
                if (message.contains(getPhaseNameByNumber(i))) {
                    markPhaseError(i, message);
                    break;
                }
            }
        }
    }
    
    /**
     * Updates the UI based on the selected job.
     */
    private void updateUIForJobSelection() {
        if (currentJob == null) {
            updateUIForNoJob();
            return;
        }
        
        boolean jobRunning = isJobRunning();
        
        // Update statistics table
        ObservableList<Map.Entry<String, Object>> stats = FXCollections.observableArrayList(
            currentJob.getStatistics().entrySet()
        );
        statisticsTable.setItems(stats);
        
        // Enable or disable buttons based on job status
        startMappingButton.setDisable(jobRunning || 
            currentJob.getMappingStatus() == ProcessingJob.ProcessingPhaseStatus.RUNNING);
        
        startValidationButton.setDisable(jobRunning || 
            currentJob.getMappingStatus() != ProcessingJob.ProcessingPhaseStatus.COMPLETED || 
            currentJob.getValidationStatus() == ProcessingJob.ProcessingPhaseStatus.RUNNING);
        
        startExtractionButton.setDisable(jobRunning || 
            currentJob.getValidationStatus() != ProcessingJob.ProcessingPhaseStatus.COMPLETED || 
            currentJob.getExtractionStatus() == ProcessingJob.ProcessingPhaseStatus.RUNNING);
        
        startEnhancementButton.setDisable(jobRunning || 
            currentJob.getExtractionStatus() != ProcessingJob.ProcessingPhaseStatus.COMPLETED || 
            currentJob.getEnhancementStatus() == ProcessingJob.ProcessingPhaseStatus.RUNNING);
        
        stopProcessingButton.setDisable(!jobRunning);
        
        // Update status label
        updateStatus("Selected job: " + currentJob.getName(), false);
    }
    
    /**
     * Updates the UI when no job is selected.
     */
    private void updateUIForNoJob() {
        statisticsTable.setItems(FXCollections.observableArrayList());
        startMappingButton.setDisable(true);
        startValidationButton.setDisable(true);
        startExtractionButton.setDisable(true);
        startEnhancementButton.setDisable(true);
        stopProcessingButton.setDisable(true);
        updateStatus("No job selected", false);
    }
    
    /**
     * Sets the UI state during processing.
     *
     * @param processing Whether processing is active
     */
    private void setProcessingUIState(boolean processing) {
        createJobButton.setDisable(processing);
        jobSelector.setDisable(processing);
        startMappingButton.setDisable(processing);
        startValidationButton.setDisable(processing);
        startExtractionButton.setDisable(processing);
        startEnhancementButton.setDisable(processing);
        stopProcessingButton.setDisable(!processing);
        
        // Update progress bar
        progressBar.setVisible(processing);
        
        // If processing is active, also display a determinate progress
        // to show the application is still responsive
        if (processing) {
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            
            // Set up a timer to regularly poll job status
            startStatusPolling();
        } else {
            progressBar.setProgress(0);
            stopStatusPolling();
        }
    }
    
    /**
     * Checks if the current job is running.
     *
     * @return True if the job is running
     */
    private boolean isJobRunning() {
        if (currentJob == null) {
            return false;
        }
        
        Map<String, Object> status = catalogProcessingService.getJobStatus(currentJob.getId());
        return status.containsKey("isRunning") && (boolean) status.get("isRunning");
    }
    
    /**
     * Updates the job data from the service.
     */
    private void refreshJobData() {
        if (currentJob != null) {
            ProcessingJob updatedJob = catalogProcessingService.getJob(currentJob.getId());
            if (updatedJob != null) {
                // Update the current job reference
                int index = jobs.indexOf(currentJob);
                if (index >= 0) {
                    jobs.set(index, updatedJob);
                    currentJob = updatedJob;
                    jobSelector.getSelectionModel().select(updatedJob);
                }
            }
        }
    }
    
    /**
     * Updates the status label.
     *
     * @param message The status message
     * @param isError Whether it's an error
     */
    private void updateStatus(String message, boolean isError) {
        statusLabel.setText(message);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: red;");
        } else {
            statusLabel.setStyle("-fx-text-fill: black;");
        }
    }
    
    /**
     * Appends a message to the log.
     *
     * @param message The message
     */
    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
        });
    }
    
    /**
     * Clears the log area.
     */
    private void clearLog() {
        logArea.clear();
    }
    
    /**
     * Handles processing results.
     *
     * @param result The processing result
     */
    private void handleResults(CatalogProcessingResult result) {
        Platform.runLater(() -> {
            // Update UI with result information
            if (result.isCompleted()) {
                appendLog("Processing completed: " + (result.isSuccessful() ? "Success" : "Failed"));
                if (!result.isSuccessful() && result.getErrorMessage() != null) {
                    appendLog("Error: " + result.getErrorMessage());
                }
            }
            
            appendLog("Processed URLs: " + result.getTotalProcessedCount());
            appendLog("Success: " + result.getSuccessCount());
            appendLog("Warnings: " + result.getWarningCount());
            appendLog("Errors: " + result.getErrorCount());
            
            // Refresh job data to update statistics
            refreshJobData();
        });
    }
    
    /**
     * Updates the UI for which phase of processing we're in.
     *
     * @param phase The current processing phase (1-5)
     * @param detail Detailed information about the current phase
     */
    private void updateProcessPhase(int phase, String detail) {
        Platform.runLater(() -> {
            // Reset all circles to waiting state
            phase1Circle.setFill(phase > 1 ? COMPLETED_COLOR : phase == 1 ? ACTIVE_COLOR : WAITING_COLOR);
            phase2Circle.setFill(phase > 2 ? COMPLETED_COLOR : phase == 2 ? ACTIVE_COLOR : WAITING_COLOR);
            phase3Circle.setFill(phase > 3 ? COMPLETED_COLOR : phase == 3 ? ACTIVE_COLOR : WAITING_COLOR);
            phase4Circle.setFill(phase > 4 ? COMPLETED_COLOR : phase == 4 ? ACTIVE_COLOR : WAITING_COLOR);
            phase5Circle.setFill(phase > 5 ? COMPLETED_COLOR : phase == 5 ? ACTIVE_COLOR : WAITING_COLOR);
            
            // Update detail label
            phaseDetailLabel.setText(detail);
        });
    }
    
    /**
     * Marks a process phase as having an error.
     *
     * @param phase The phase with an error (1-5)
     * @param errorMessage The error message
     */
    private void markPhaseError(int phase, String errorMessage) {
        Platform.runLater(() -> {
            // Mark the specified phase as error
            switch (phase) {
                case 1 -> phase1Circle.setFill(ERROR_COLOR);
                case 2 -> phase2Circle.setFill(ERROR_COLOR);
                case 3 -> phase3Circle.setFill(ERROR_COLOR);
                case 4 -> phase4Circle.setFill(ERROR_COLOR);
                case 5 -> phase5Circle.setFill(ERROR_COLOR);
            }
            
            // Update detail label with error message
            phaseDetailLabel.setText("Error in " + getPhaseNameByNumber(phase) + ": " + errorMessage);
        });
    }
    
    /**
     * Gets the name of a phase by its number.
     *
     * @param phase The phase number (1-5)
     * @return The name of the phase
     */
    private String getPhaseNameByNumber(int phase) {
        return switch (phase) {
            case 1 -> "AI Reconnaissance";
            case 2 -> "Optimized Setup";
            case 3 -> "Smart Crawling";
            case 4 -> "Validation & Enrichment";
            case 5 -> "Results Processing";
            default -> "Unknown Phase";
        };
    }
    
    /**
     * Resets the process phase indicators.
     */
    private void resetProcessPhases() {
        Platform.runLater(() -> {
            phase1Circle.setFill(WAITING_COLOR);
            phase2Circle.setFill(WAITING_COLOR);
            phase3Circle.setFill(WAITING_COLOR);
            phase4Circle.setFill(WAITING_COLOR);
            phase5Circle.setFill(WAITING_COLOR);
            phaseDetailLabel.setText("Select a job and start mapping to begin the AI-enhanced process");
        });
    }
    
    /**
     * Starts polling for job status updates.
     */
    private void startStatusPolling() {
        // Stop any existing timer
        stopStatusPolling();
        
        // Create a new timer that polls every 2 seconds
        statusPollTimer = new java.util.Timer();
        statusPollTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                if (currentJob != null) {
                    // Check if the job is still running
                    Map<String, Object> status = catalogProcessingService.getJobStatus(currentJob.getId());
                    
                    if (status.containsKey("isRunning") && (boolean) status.get("isRunning")) {
                        // Update statistics on the JavaFX thread
                        Platform.runLater(() -> {
                            refreshJobData();
                            updateUIForJobSelection();
                        });
                    } else {
                        // If the job is no longer running, stop polling
                        stopStatusPolling();
                    }
                }
            }
        }, 2000, 2000); // Initial delay: 2 sec, Period: 2 sec
    }
    
    /**
     * Stops polling for job status updates.
     */
    private void stopStatusPolling() {
        if (statusPollTimer != null) {
            statusPollTimer.cancel();
            statusPollTimer = null;
        }
    }
    
    /**
     * Cleans up resources before application shutdown.
     * This should be called by the MainApp when the application is stopping.
     */
    public void shutdown() {
        // Stop status polling
        stopStatusPolling();
        
        // Cancel any active processing task
        if (activeProcessingTask != null && !activeProcessingTask.isDone()) {
            // Try to stop the running job
            if (currentJob != null) {
                catalogProcessingService.stopJob(currentJob.getId());
            }
        }
    }
} 