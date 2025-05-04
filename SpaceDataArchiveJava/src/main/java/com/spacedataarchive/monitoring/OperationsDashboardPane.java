package com.spacedataarchive.monitoring;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

/**
 * Operations Dashboard panel for monitoring microservices and Kubernetes resources.
 */
public class OperationsDashboardPane extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(OperationsDashboardPane.class);
    
    // Services Overview
    private VBox servicesOverviewBox;
    private GridPane servicesGrid;
    
    // Client Connections
    private VBox clientConnectionsBox;
    private TableView<ClientConnectionStatus> clientConnectionsTable;
    private Label totalConnLabel;
    private Label activeConnLabel;
    private Label idleConnLabel;
    
    // Kubernetes Resources
    private VBox kubernetesResourcesBox;
    private TableView<KubernetesResource> kubernetesTable;
    
    // Metrics View
    private VBox metricsBox;
    private GridPane metricsGrid;
    
    // Logs Console
    private VBox logsBox;
    private TextArea logsTextArea;
    
    // Service monitoring
    private ServiceMonitoringService monitoringService;
    private AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private Timer monitoringTimer;
    
    // Kubernetes Resources pane
    private KubernetesResourcesPane kubernetesResourcesPane;
    
    // URL Validation
    private int totalValidations = 0;
    private int successfulValidations = 0;
    private long totalResponseTime = 0;
    
    // Tab pane
    private TabPane tabPane;
    
    /**
     * Constructs an Operations Dashboard with all components.
     */
    public OperationsDashboardPane() {
        initialize();
    }
    
    /**
     * Initializes the dashboard components.
     */
    private void initialize() {
        logger.info("Initializing Operations Dashboard");
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #f5f5f5;");
        
        // Initialize services
        monitoringService = new ServiceMonitoringService();
        
        // Create layout components
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Create tabs
        Tab servicesTab = new Tab("Services Overview");
        servicesTab.setContent(createServicesOverviewPane());
        
        Tab clientsTab = new Tab("Client Connections");
        clientsTab.setContent(createClientConnectionsPane());
        
        Tab kubernetesTab = new Tab("Kubernetes Resources");
        kubernetesTab.setContent(createKubernetesResourcesPane());
        
        Tab metricsTab = new Tab("Metrics View");
        metricsTab.setContent(createMetricsPane());
        
        Tab logsTab = new Tab("Logs Console");
        logsTab.setContent(createLogsPane());
        
        Tab urlValidationTab = new Tab("URL Validation");
        urlValidationTab.setContent(createUrlValidationPane());
        
        // Add tabs to pane
        tabPane.getTabs().addAll(
            servicesTab, 
            clientsTab, 
            kubernetesTab, 
            metricsTab, 
            logsTab,
            urlValidationTab
        );
        
        // Set main content
        setCenter(tabPane);
        
        // Add dashboard title and status indicator
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setPadding(new Insets(0, 0, 10, 0));
        
        Label titleLabel = new Label("Operations Dashboard");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label statusLabel = new Label("Dashboard Ready");
        statusLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Create dashboard controls
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> refreshData());
        
        titleBox.getChildren().addAll(titleLabel, spacer, statusLabel, refreshButton);
        setTop(titleBox);
        
        logger.info("Operations Dashboard initialized");
    }
    
    /**
     * Creates the Services Overview panel showing health of microservices.
     */
    private Parent createServicesOverviewPane() {
        servicesOverviewBox = new VBox(10);
        servicesOverviewBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("Microservices Health Status");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        servicesGrid = new GridPane();
        servicesGrid.setHgap(15);
        servicesGrid.setVgap(10);
        servicesGrid.setPadding(new Insets(10));
        
        // Add header row
        Label nameHeader = new Label("Service");
        nameHeader.setStyle("-fx-font-weight: bold;");
        Label statusHeader = new Label("Status");
        statusHeader.setStyle("-fx-font-weight: bold;");
        Label lastCheckedHeader = new Label("Last Checked");
        lastCheckedHeader.setStyle("-fx-font-weight: bold;");
        Label endpointsHeader = new Label("API Endpoints");
        endpointsHeader.setStyle("-fx-font-weight: bold;");
        
        servicesGrid.add(nameHeader, 0, 0);
        servicesGrid.add(statusHeader, 1, 0);
        servicesGrid.add(lastCheckedHeader, 2, 0);
        servicesGrid.add(endpointsHeader, 3, 0);
        
        // Add placeholder services
        addServiceToGrid("Eureka Server", ServiceHealthStatus.Status.UNKNOWN, "Not checked", 1);
        addServiceToGrid("API Gateway", ServiceHealthStatus.Status.UNKNOWN, "Not checked", 2);
        addServiceToGrid("URL Validation Service", ServiceHealthStatus.Status.UNKNOWN, "Not checked", 3);
        addServiceToGrid("Catalog Processor", ServiceHealthStatus.Status.UNKNOWN, "Not checked", 4);
        addServiceToGrid("NLP Service", ServiceHealthStatus.Status.UNKNOWN, "Not checked", 5);
        
        servicesOverviewBox.getChildren().addAll(sectionTitle, servicesGrid);
        
        ScrollPane scrollPane = new ScrollPane(servicesOverviewBox);
        scrollPane.setFitToWidth(true);
        
        return scrollPane;
    }
    
    /**
     * Adds a service to the services grid.
     */
    private void addServiceToGrid(String serviceName, ServiceHealthStatus.Status status, String lastChecked, int row) {
        Label nameLabel = new Label(serviceName);
        
        // Status indicator (traffic light)
        Pane statusIndicator = new Pane();
        statusIndicator.setPrefSize(20, 20);
        statusIndicator.setStyle("-fx-background-radius: 10; -fx-background-color: " + getStatusColor(status));
        
        Label lastCheckedLabel = new Label(lastChecked);
        
        // Add API endpoints info based on service name
        String endpointsInfo = getEndpointsForService(serviceName);
        Label endpointsLabel = new Label(endpointsInfo);
        endpointsLabel.setWrapText(true);
        endpointsLabel.setPrefWidth(200);
        
        servicesGrid.add(nameLabel, 0, row);
        servicesGrid.add(statusIndicator, 1, row);
        servicesGrid.add(lastCheckedLabel, 2, row);
        servicesGrid.add(endpointsLabel, 3, row);
    }
    
    /**
     * Gets API endpoints information for a service.
     */
    private String getEndpointsForService(String serviceName) {
        switch (serviceName) {
            case "URL Validation Service":
                return "http://localhost:8083/api/v1/validate/url\n" +
                       "http://localhost:8083/api/v1/validate/urls/batch";
            case "API Gateway":
                return "http://localhost:8080/api/*";
            case "Eureka Server":
                return "http://localhost:8761/eureka/";
            case "Catalog Processor":
                return "http://localhost:8082/api/v1/catalog/*";
            case "NLP Service":
                return "http://localhost:8081/api/v1/nlp/*";
            default:
                return "No endpoints available";
        }
    }
    
    /**
     * Get CSS color for status.
     */
    private String getStatusColor(ServiceHealthStatus.Status status) {
        switch (status) {
            case HEALTHY:
                return "#2ecc71"; // Green
            case DEGRADED:
                return "#f39c12"; // Yellow/Orange
            case DOWN:
                return "#e74c3c"; // Red
            case UNKNOWN:
            default:
                return "#95a5a6"; // Gray
        }
    }
    
    /**
     * Creates the Kubernetes Resources panel.
     */
    private Parent createKubernetesResourcesPane() {
        kubernetesResourcesBox = new VBox(10);
        kubernetesResourcesBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("Kubernetes Resources");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Use the new specialized KubernetesResourcesPane
        KubernetesResourcesPane resourcesPane = new KubernetesResourcesPane();
        VBox.setVgrow(resourcesPane, Priority.ALWAYS);
        
        // Store a reference to enable refreshing the data later
        kubernetesResourcesPane = resourcesPane;
        
        kubernetesResourcesBox.getChildren().addAll(sectionTitle, resourcesPane);
        
        ScrollPane scrollPane = new ScrollPane(kubernetesResourcesBox);
        scrollPane.setFitToWidth(true);
        
        return scrollPane;
    }
    
    /**
     * Creates the Metrics View panel.
     */
    private Parent createMetricsPane() {
        metricsBox = new VBox(10);
        metricsBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("Performance Metrics");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label placeholderLabel = new Label("Metrics integration coming soon");
        placeholderLabel.setStyle("-fx-font-style: italic;");
        
        metricsBox.getChildren().addAll(sectionTitle, placeholderLabel);
        
        ScrollPane scrollPane = new ScrollPane(metricsBox);
        scrollPane.setFitToWidth(true);
        
        return scrollPane;
    }
    
    /**
     * Creates the Logs Console panel.
     */
    private Pane createLogsPane() {
        logsBox = new VBox(10);
        logsBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("Services Logs");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        logsTextArea = new TextArea();
        logsTextArea.setEditable(false);
        logsTextArea.setPrefRowCount(20);
        logsTextArea.setWrapText(true);
        
        logsTextArea.setText("Log integration coming soon...\n");
        
        logsBox.getChildren().addAll(sectionTitle, logsTextArea);
        
        return logsBox;
    }
    
    /**
     * Creates the Client Connections panel showing active client connections.
     */
    private Parent createClientConnectionsPane() {
        clientConnectionsBox = new VBox(10);
        clientConnectionsBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("Active Client Connections");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Create a table view for client connections
        clientConnectionsTable = new TableView<>();
        clientConnectionsTable.setPlaceholder(new Label("No client connections found"));
        
        // Create table columns
        TableColumn<ClientConnectionStatus, String> clientIdColumn = new TableColumn<>("Client ID");
        clientIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClientId()));
        clientIdColumn.setPrefWidth(100);
        
        TableColumn<ClientConnectionStatus, String> clientIpColumn = new TableColumn<>("IP Address");
        clientIpColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getClientIp()));
        clientIpColumn.setPrefWidth(120);
        
        TableColumn<ClientConnectionStatus, String> serviceColumn = new TableColumn<>("Connected Service");
        serviceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConnectedService()));
        serviceColumn.setPrefWidth(150);
        
        TableColumn<ClientConnectionStatus, String> connectedSinceColumn = new TableColumn<>("Connected Since");
        connectedSinceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConnectedSince()));
        connectedSinceColumn.setPrefWidth(110);
        
        TableColumn<ClientConnectionStatus, String> stateColumn = new TableColumn<>("State");
        stateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConnectionState().toString()));
        stateColumn.setPrefWidth(90);
        
        // Add colored status indicator for state
        stateColumn.setCellFactory(column -> new TableCell<ClientConnectionStatus, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Set background color based on state
                    if (item.equals(ClientConnectionStatus.ConnectionState.ACTIVE.toString())) {
                        setStyle("-fx-background-color: #e6fff2;"); // Light green
                    } else if (item.equals(ClientConnectionStatus.ConnectionState.IDLE.toString())) {
                        setStyle("-fx-background-color: #fffde6;"); // Light yellow
                    } else if (item.equals(ClientConnectionStatus.ConnectionState.DISCONNECTED.toString())) {
                        setStyle("-fx-background-color: #f2f2f2;"); // Light gray
                    } else if (item.equals(ClientConnectionStatus.ConnectionState.ERROR.toString())) {
                        setStyle("-fx-background-color: #ffebeb;"); // Light red
                    } else {
                        setStyle("");
                    }
                }
            }
        });
        
        TableColumn<ClientConnectionStatus, String> lastActivityColumn = new TableColumn<>("Last Activity");
        lastActivityColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastActivity()));
        lastActivityColumn.setPrefWidth(110);
        
        // Add the columns to the table
        clientConnectionsTable.getColumns().addAll(
                clientIdColumn, clientIpColumn, serviceColumn, 
                connectedSinceColumn, stateColumn, lastActivityColumn);
        
        VBox.setVgrow(clientConnectionsTable, Priority.ALWAYS);
        
        // Add client stats section
        Label statsTitle = new Label("Connection Statistics");
        statsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);
        statsGrid.setPadding(new Insets(10));
        
        // Add stats labels
        totalConnLabel = new Label("0");
        activeConnLabel = new Label("0");
        idleConnLabel = new Label("0");
        
        // Add stats rows
        addStatsRow(statsGrid, "Total Connections:", totalConnLabel, 0);
        addStatsRow(statsGrid, "Active Connections:", activeConnLabel, 1);
        addStatsRow(statsGrid, "Idle Connections:", idleConnLabel, 2);
        
        // Add components to the main box
        clientConnectionsBox.getChildren().addAll(sectionTitle, clientConnectionsTable, statsTitle, statsGrid);
        
        // Create a scroll pane to contain all content
        ScrollPane scrollPane = new ScrollPane(clientConnectionsBox);
        scrollPane.setFitToWidth(true);
        
        return scrollPane;
    }
    
    /**
     * Adds a statistics row to the grid.
     */
    private void addStatsRow(GridPane grid, String labelText, Label valueLabel, int row) {
        Label nameLabel = new Label(labelText);
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }
    
    /**
     * Updates the client connections table with the provided data.
     */
    private void updateClientConnectionsTable(List<ClientConnectionStatus> connections) {
        if (clientConnectionsTable != null) {
            clientConnectionsTable.setItems(FXCollections.observableArrayList(connections));
            
            // Update connection statistics
            int totalCount = connections.size();
            int activeCount = 0;
            int idleCount = 0;
            
            for (ClientConnectionStatus connection : connections) {
                if (connection.getConnectionState() == ClientConnectionStatus.ConnectionState.ACTIVE) {
                    activeCount++;
                } else if (connection.getConnectionState() == ClientConnectionStatus.ConnectionState.IDLE) {
                    idleCount++;
                }
            }
            
            // Update stats labels
            totalConnLabel.setText(String.valueOf(totalCount));
            activeConnLabel.setText(String.valueOf(activeCount));
            idleConnLabel.setText(String.valueOf(idleCount));
        }
    }
    
    /**
     * Starts periodic monitoring of services.
     */
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            logger.info("Starting service monitoring");
            
            // Start periodic updates
            monitoringTimer = new Timer(true);
            monitoringTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    refreshData();
                }
            }, 0, 5000); // Update every 5 seconds
        }
    }
    
    /**
     * Stops monitoring services.
     */
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            logger.info("Stopping service monitoring");
            
            if (monitoringTimer != null) {
                monitoringTimer.cancel();
                monitoringTimer = null;
            }
        }
    }
    
    /**
     * Refreshes all dashboard data.
     */
    public void refreshData() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshData);
            return;
        }
        
        logger.info("Refreshing Operations Dashboard data");
        
        try {
            // Update service statuses
            List<ServiceHealthStatus> serviceStatuses = monitoringService.checkAllServices();
            updateServicesGrid(serviceStatuses);
            
            // Update client connections
            List<ClientConnectionStatus> clientConnections = monitoringService.getClientConnections();
            updateClientConnectionsTable(clientConnections);
            
            // Update Kubernetes resources
            List<KubernetesResource> resources = monitoringService.getKubernetesResources();
            updateKubernetesTable(resources);
            
        } catch (Exception e) {
            logger.error("Error refreshing dashboard data", e);
        }
    }
    
    /**
     * Updates the services grid with current status information.
     */
    private void updateServicesGrid(List<ServiceHealthStatus> serviceStatuses) {
        // Clear existing rows except header
        int rowCount = servicesGrid.getRowCount();
        for (int i = rowCount - 1; i > 0; i--) {
            final int rowIndex = i; // Create a final copy for the lambda
            servicesGrid.getChildren().removeIf(node -> 
                    GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == rowIndex);
        }
        
        // Add or update services
        int row = 1;
        for (ServiceHealthStatus status : serviceStatuses) {
            String nameWithPort = status.getServiceName();
            if (status.getPort() > 0) {
                nameWithPort += " (" + status.getPort() + ")";
            }
            
            addServiceToGrid(nameWithPort, status.getStatus(), 
                    status.getLastChecked(), row++);
        }
    }
    
    /**
     * Updates the Kubernetes resources table.
     */
    private void updateKubernetesTable(List<KubernetesResource> resources) {
        if (kubernetesResourcesPane != null) {
            kubernetesResourcesPane.updateResources(resources);
        }
    }

    /**
     * Creates the URL Validation panel.
     */
    private Parent createUrlValidationPane() {
        VBox urlValidationBox = new VBox(10);
        urlValidationBox.setPadding(new Insets(10));
        
        Label sectionTitle = new Label("URL Validation Service");
        sectionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // URL input
        TextField urlInput = new TextField();
        urlInput.setPromptText("Enter URL to validate");
        
        // Validation options
        CheckBox followRedirectsCheck = new CheckBox("Follow Redirects");
        followRedirectsCheck.setSelected(true);
        
        Spinner<Integer> timeoutSpinner = new Spinner<>(1, 60, 5);
        timeoutSpinner.setEditable(true);
        Label timeoutLabel = new Label("Timeout (seconds):");
        
        HBox optionsBox = new HBox(10);
        optionsBox.setAlignment(Pos.CENTER_LEFT);
        optionsBox.getChildren().addAll(followRedirectsCheck, timeoutLabel, timeoutSpinner);
        
        // Validation button
        Button validateButton = new Button("Validate URL");
        validateButton.setOnAction(e -> validateUrl(urlInput.getText(), 
            followRedirectsCheck.isSelected(), 
            timeoutSpinner.getValue()));
        
        // Results area
        TextArea resultsArea = new TextArea();
        resultsArea.setEditable(false);
        resultsArea.setPrefRowCount(10);
        resultsArea.setWrapText(true);
        
        // Statistics
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10);
        statsGrid.setVgap(5);
        statsGrid.setPadding(new Insets(10));
        
        Label totalValidatedLabel = new Label("Total URLs Validated:");
        Label successRateLabel = new Label("Success Rate:");
        Label avgResponseTimeLabel = new Label("Avg Response Time:");
        
        Label totalValidatedValue = new Label("0");
        Label successRateValue = new Label("0%");
        Label avgResponseTimeValue = new Label("0ms");
        
        statsGrid.addRow(0, totalValidatedLabel, totalValidatedValue);
        statsGrid.addRow(1, successRateLabel, successRateValue);
        statsGrid.addRow(2, avgResponseTimeLabel, avgResponseTimeValue);
        
        // Add components to main box
        urlValidationBox.getChildren().addAll(
            sectionTitle,
            urlInput,
            optionsBox,
            validateButton,
            new Separator(),
            new Label("Validation Results:"),
            resultsArea,
            new Separator(),
            new Label("Statistics:"),
            statsGrid
        );
        
        return new ScrollPane(urlValidationBox);
    }
    
    /**
     * Validates a URL using the URL Validation Service.
     */
    private void validateUrl(String url, boolean followRedirects, int timeoutSeconds) {
        if (url == null || url.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter a URL");
            return;
        }
        
        // Create HTTP client
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
            
        // Create request body
        JsonObject requestBody = Json.createObjectBuilder()
            .add("url", url)
            .add("followRedirects", followRedirects)
            .add("connectTimeoutMs", timeoutSeconds * 1000)
            .add("socketTimeoutMs", timeoutSeconds * 1000)
            .build();
            
        // Create request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8083/api/v1/validate/url"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();
            
        // Send request asynchronously
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    try {
                        if (response.statusCode() == 200) {
                            JsonObject jsonResponse = Json.createReader(
                                new StringReader(response.body())).readObject();
                            updateValidationResults(jsonResponse);
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Validation Error", 
                                "Failed to validate URL: " + response.statusCode());
                        }
                    } catch (Exception e) {
                        logger.error("Error processing validation response", e);
                        showAlert(Alert.AlertType.ERROR, "Error", 
                            "Failed to process validation response: " + e.getMessage());
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    logger.error("Error validating URL", e);
                    showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to validate URL: " + e.getMessage());
                });
                return null;
            });
    }
    
    /**
     * Updates the validation results display.
     */
    private void updateValidationResults(JsonObject response) {
        JsonObject data = response.getJsonObject("data");
        if (data == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid response format");
            return;
        }
        
        // Update results area
        StringBuilder resultText = new StringBuilder();
        resultText.append("URL: ").append(data.getString("url", "N/A")).append("\n");
        resultText.append("Valid: ").append(data.getBoolean("valid", false)).append("\n");
        resultText.append("Status Code: ").append(data.getInt("statusCode", -1)).append("\n");
        resultText.append("Content Type: ").append(data.getString("contentType", "N/A")).append("\n");
        resultText.append("Response Time: ").append(data.getInt("responseTimeMs", -1)).append("ms\n");
        
        if (data.getBoolean("redirect", false)) {
            resultText.append("Redirect URL: ").append(data.getString("redirectUrl", "N/A")).append("\n");
        }
        
        if (data.containsKey("error")) {
            resultText.append("Error: ").append(data.getString("error")).append("\n");
        }
        
        // Find the results area and update it
        Node urlValidationPane = tabPane.getSelectionModel().getSelectedItem().getContent();
        if (urlValidationPane instanceof ScrollPane) {
            Node content = ((ScrollPane) urlValidationPane).getContent();
            if (content instanceof VBox) {
                VBox box = (VBox) content;
                for (Node node : box.getChildren()) {
                    if (node instanceof TextArea) {
                        ((TextArea) node).setText(resultText.toString());
                        break;
                    }
                }
            }
        }
        
        // Update statistics
        updateValidationStatistics(data);
    }
    
    /**
     * Updates the validation statistics display.
     */
    private void updateValidationStatistics(JsonObject data) {
        // Find the statistics grid
        Node urlValidationPane = tabPane.getSelectionModel().getSelectedItem().getContent();
        if (!(urlValidationPane instanceof ScrollPane)) return;
        
        Node content = ((ScrollPane) urlValidationPane).getContent();
        if (!(content instanceof VBox)) return;
        
        VBox box = (VBox) content;
        GridPane statsGrid = null;
        for (Node node : box.getChildren()) {
            if (node instanceof GridPane) {
                statsGrid = (GridPane) node;
                break;
            }
        }
        
        if (statsGrid == null) return;
        
        // Update statistics labels
        for (Node node : statsGrid.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                int row = GridPane.getRowIndex(label);
                int col = GridPane.getColumnIndex(label);
                
                if (col == 1) { // Value column
                    switch (row) {
                        case 0: // Total URLs
                            totalValidations++;
                            label.setText(String.valueOf(totalValidations));
                            break;
                        case 1: // Success Rate
                            if (data.getBoolean("valid", false)) successfulValidations++;
                            double successRate = (totalValidations > 0) ? 
                                (successfulValidations * 100.0 / totalValidations) : 0;
                            label.setText(String.format("%.1f%%", successRate));
                            break;
                        case 2: // Avg Response Time
                            totalResponseTime += data.getInt("responseTimeMs", 0);
                            double avgResponseTime = (totalValidations > 0) ? 
                                (totalResponseTime / totalValidations) : 0;
                            label.setText(String.format("%.0fms", avgResponseTime));
                            break;
                    }
                }
            }
        }
    }
    
    /**
     * Shows an alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 