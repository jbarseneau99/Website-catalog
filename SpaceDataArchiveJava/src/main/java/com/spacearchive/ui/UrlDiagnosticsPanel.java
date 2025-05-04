package com.spacearchive.ui;

import com.spacearchive.diagnostics.DiagnosticResult;
import com.spacearchive.diagnostics.UrlDiagnosticClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for URL Diagnostics functionality that matches the existing Java UI
 */
public class UrlDiagnosticsPanel extends JPanel {
    
    private static final Logger LOGGER = Logger.getLogger(UrlDiagnosticsPanel.class.getName());
    
    private final UrlDiagnosticClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    // UI components
    private JTextField urlField;
    private JButton analyzeButton;
    private JPanel resultsPanel;
    private JTextArea rawResultsArea;
    private JList<String> recentUrlsList;
    private DefaultListModel<String> recentUrlsModel;
    private JPanel loadingPanel;
    private JLabel statusLabel;
    
    /**
     * Creates a new URL Diagnostics panel
     */
    public UrlDiagnosticsPanel() {
        LOGGER.info("Initializing URL Diagnostics Panel");
        this.client = new UrlDiagnosticClient("http://localhost:3000");
        initializeUI();
        
        // Check if service is available
        if (!client.isServiceAvailable()) {
            LOGGER.severe("URL Diagnostic service is not available");
            showError("URL Diagnostic service is not available at http://localhost:3000");
            statusLabel.setText("Error: Service unavailable");
            analyzeButton.setEnabled(false);
        } else {
            LOGGER.info("URL Diagnostic service is available");
            loadRecentUrls();
            statusLabel.setText("Connected to diagnostic service");
        }
    }
    
    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        LOGGER.fine("Setting up UI components");
        // Set panel properties
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create top panel with URL input field
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel urlLabel = new JLabel("URL to analyze:");
        urlField = new JTextField();
        analyzeButton = new JButton("Run Diagnostics");
        
        topPanel.add(urlLabel, BorderLayout.WEST);
        topPanel.add(urlField, BorderLayout.CENTER);
        topPanel.add(analyzeButton, BorderLayout.EAST);
        
        // Create main content panel with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7); // 70% for results, 30% for recent URLs
        
        // Create results panel
        resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Diagnostics Results"));
        
        // Create raw results text area
        rawResultsArea = new JTextArea();
        rawResultsArea.setEditable(false);
        rawResultsArea.setLineWrap(true);
        rawResultsArea.setWrapStyleWord(true);
        rawResultsArea.setText("Enter a URL and click 'Run Diagnostics' to analyze it");
        
        JScrollPane resultsScrollPane = new JScrollPane(rawResultsArea);
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);
        
        // Create loading panel
        loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setVisible(false);
        JLabel loadingLabel = new JLabel("Analyzing URL...");
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        
        resultsPanel.add(loadingPanel, BorderLayout.NORTH);
        
        // Create recent URLs panel
        JPanel recentUrlsPanel = new JPanel(new BorderLayout());
        recentUrlsPanel.setBorder(BorderFactory.createTitledBorder("Recent URLs"));
        
        recentUrlsModel = new DefaultListModel<>();
        recentUrlsList = new JList<>(recentUrlsModel);
        recentUrlsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane recentUrlsScrollPane = new JScrollPane(recentUrlsList);
        recentUrlsPanel.add(recentUrlsScrollPane, BorderLayout.CENTER);
        
        JButton useSelectedButton = new JButton("Use Selected URL");
        useSelectedButton.addActionListener(e -> {
            String selectedUrl = recentUrlsList.getSelectedValue();
            if (selectedUrl != null) {
                urlField.setText(selectedUrl);
            }
        });
        
        recentUrlsPanel.add(useSelectedButton, BorderLayout.SOUTH);
        
        // Add panels to split pane
        splitPane.setLeftComponent(resultsPanel);
        splitPane.setRightComponent(recentUrlsPanel);
        
        // Create status bar
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusLabel = new JLabel("Initializing...");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        // Add components to main panel
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
        // Add action listeners
        analyzeButton.addActionListener(e -> analyzeUrl());
        
        recentUrlsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUrl = recentUrlsList.getSelectedValue();
                if (selectedUrl != null) {
                    urlField.setText(selectedUrl);
                }
            }
        });
        
        LOGGER.fine("UI components initialization complete");
    }
    
    /**
     * Load recent URLs from the client
     */
    private void loadRecentUrls() {
        LOGGER.fine("Loading recent URLs");
        executor.submit(() -> {
            try {
                List<String> recentUrls = client.getRecentUrls();
                LOGGER.info("Loaded " + recentUrls.size() + " recent URLs");
                
                SwingUtilities.invokeLater(() -> {
                    recentUrlsModel.clear();
                    for (String url : recentUrls) {
                        recentUrlsModel.addElement(url);
                    }
                    statusLabel.setText("Loaded " + recentUrls.size() + " recent URLs");
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading recent URLs", e);
                showError("Error loading recent URLs: " + e.getMessage());
            }
        });
    }
    
    /**
     * Analyze the URL entered in the URL field
     */
    private void analyzeUrl() {
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showError("Please enter a URL to analyze");
            return;
        }
        
        LOGGER.info("Analyzing URL: " + url);
        // Show loading state
        setAnalyzing(true);
        
        // Execute the analysis in a background thread
        executor.submit(() -> {
            try {
                LOGGER.fine("Sending URL for analysis: " + url);
                DiagnosticResult result = client.analyzeUrl(url);
                LOGGER.info("Analysis complete for URL: " + url);
                
                // Update UI in the EDT
                SwingUtilities.invokeLater(() -> {
                    displayResults(result);
                    loadRecentUrls(); // Refresh recent URLs
                    setAnalyzing(false);
                    statusLabel.setText("Analysis complete");
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error analyzing URL: " + url, e);
                SwingUtilities.invokeLater(() -> {
                    showError("Error analyzing URL: " + e.getMessage());
                    setAnalyzing(false);
                    statusLabel.setText("Error analyzing URL");
                });
            }
        });
    }
    
    /**
     * Display the analysis results
     */
    private void displayResults(DiagnosticResult result) {
        // Show pretty-formatted results in a structured way
        StringBuilder sb = new StringBuilder();
        
        sb.append("URL: ").append(result.getUrl()).append("\n");
        sb.append("Status Code: ").append(result.getStatusCode()).append("\n");
        
        if (result.isRedirected()) {
            sb.append("Redirected to: ").append(result.getRedirectUrl()).append("\n");
        }
        
        sb.append("\n=== Page Information ===\n");
        sb.append("Page Type: ").append(result.getDeepAnalysis() != null ? 
                result.getDeepAnalysis().getPageType() : "Unknown").append("\n");
        sb.append("Content Type: ").append(result.getContentType()).append("\n");
        sb.append("Content Length: ").append(result.getContentLength()).append(" bytes\n");
        
        sb.append("\n=== Asset Detection ===\n");
        if (result.getAssetCounts() != null) {
            sb.append("Documents: ").append(result.getAssetCounts().getDocumentCount()).append("\n");
            sb.append("Images: ").append(result.getAssetCounts().getImageCount()).append("\n");
            sb.append("Total Assets: ").append(result.getAssetCounts().getTotalAssets()).append("\n");
            sb.append("Is Archive Section: ").append(result.getAssetCounts().isArchiveSection() ? "Yes" : "No").append("\n");
            
            if (result.getAssetCounts().isArchiveSection()) {
                sb.append("Archive Type: ").append(result.getAssetCounts().getArchiveType()).append("\n");
                sb.append("Archive Score: ").append(String.format("%.2f", result.getAssetCounts().getArchiveScore())).append("\n");
            }
        }
        
        // Add dynamic content section if available
        if (result.getDeepAnalysis() != null && result.getDeepAnalysis().getDynamicLoading() != null) {
            sb.append("\n=== Dynamic Content ===\n");
            sb.append("AJAX Loaders: ").append(result.getDeepAnalysis().getDynamicLoading().isHasAjaxLoaders() ? "Yes" : "No").append("\n");
            sb.append("Lazy Loading: ").append(result.getDeepAnalysis().getDynamicLoading().isHasLazyLoading() ? "Yes" : "No").append("\n");
            sb.append("JavaScript Loaders: ").append(result.getDeepAnalysis().getDynamicLoading().getJsLoaders()).append("\n");
            sb.append("JavaScript Includes: ").append(result.getDeepAnalysis().getDynamicLoading().getJsIncludes()).append("\n");
        }
        
        // Add potential issues section if available
        if (result.getDeepAnalysis() != null && result.getDeepAnalysis().getPotentialIssues() != null &&
                !result.getDeepAnalysis().getPotentialIssues().isEmpty()) {
            sb.append("\n=== Potential Issues ===\n");
            for (DiagnosticResult.PotentialIssue issue : result.getDeepAnalysis().getPotentialIssues()) {
                sb.append("* ").append(issue.getType()).append(":\n");
                sb.append("  ").append(issue.getDescription()).append("\n");
                
                if (issue.getIframeUrls() != null && !issue.getIframeUrls().isEmpty()) {
                    sb.append("  Iframe URLs:\n");
                    for (String iframeUrl : issue.getIframeUrls()) {
                        sb.append("   - ").append(iframeUrl).append("\n");
                    }
                }
                
                if (issue.getContainers() != null && !issue.getContainers().isEmpty()) {
                    sb.append("  Detected containers:\n");
                    for (Map.Entry<String, Integer> entry : issue.getContainers().entrySet()) {
                        if (entry.getValue() > 0) {
                            sb.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                    }
                }
            }
        }
        
        rawResultsArea.setText(sb.toString());
        rawResultsArea.setCaretPosition(0); // Scroll to top
        LOGGER.fine("Results displayed for URL: " + result.getUrl());
    }
    
    /**
     * Display an error message
     */
    private void showError(String message) {
        LOGGER.warning("Error: " + message);
        rawResultsArea.setText("ERROR: " + message);
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Set the analyzing state
     */
    private void setAnalyzing(boolean analyzing) {
        loadingPanel.setVisible(analyzing);
        analyzeButton.setEnabled(!analyzing);
        if (analyzing) {
            rawResultsArea.setText("Analyzing URL, please wait...");
            statusLabel.setText("Analyzing URL...");
        }
    }
    
    /**
     * Clean up resources when panel is destroyed
     */
    public void cleanup() {
        LOGGER.info("Cleaning up resources");
        executor.shutdown();
    }
} 