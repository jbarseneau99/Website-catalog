package com.spacedataarchive.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spacedataarchive.monitoring.OperationsDashboardPane;

/**
 * Controller for the Operations Dashboard view.
 */
public class OperationsDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(OperationsDashboardController.class);
    
    @FXML private OperationsDashboardPane operationsDashboardPane;
    @FXML private Button refreshButton;
    
    @FXML
    private void initialize() {
        logger.info("Initializing OperationsDashboardController");
    }
    
    @FXML
    private void handleRefresh() {
        logger.info("Refreshing Operations Dashboard");
        if (operationsDashboardPane != null) {
            operationsDashboardPane.refreshData();
        }
    }
    
    /**
     * Starts monitoring services
     */
    public void startMonitoring() {
        if (operationsDashboardPane != null) {
            operationsDashboardPane.startMonitoring();
        }
    }
    
    /**
     * Stops monitoring services
     */
    public void stopMonitoring() {
        if (operationsDashboardPane != null) {
            operationsDashboardPane.stopMonitoring();
        }
    }
} 