package com.spacedataarchive;

import com.spacedataarchive.monitoring.OperationsDashboardPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple program to launch only the Operations Dashboard.
 * This lets us view the Operations Dashboard with the client connections
 * monitoring tab without launching the full application.
 */
public class LaunchOperationsDashboard extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Create the operations dashboard
        OperationsDashboardPane operationsDashboard = new OperationsDashboardPane();
        
        // Create a new stage for the dashboard
        primaryStage.setTitle("Space Data Archive System - Operations Dashboard");
        primaryStage.setScene(new Scene(operationsDashboard, 1200, 800));
        
        // Start monitoring when the stage is shown
        primaryStage.setOnShown(e -> operationsDashboard.startMonitoring());
        
        // Stop monitoring when the stage is closed
        primaryStage.setOnCloseRequest(e -> operationsDashboard.stopMonitoring());
        
        // Show the dashboard
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 