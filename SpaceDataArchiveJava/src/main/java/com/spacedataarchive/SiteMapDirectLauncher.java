package com.spacedataarchive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Direct launcher for the SiteMap view to troubleshoot the functionality
 */
public class SiteMapDirectLauncher extends Application {
    private static final Logger logger = LoggerFactory.getLogger(SiteMapDirectLauncher.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the SiteMap view directly
            logger.info("Loading SiteMap view directly");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tabs/SiteMapView.fxml"));
            BorderPane siteMapView = loader.load();
            
            // Create a scene with the SiteMap view
            Scene scene = new Scene(siteMapView, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            
            // Set up the stage
            primaryStage.setTitle("Site Map Creation - Direct Launch");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            logger.info("SiteMap view loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load SiteMap view", e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 