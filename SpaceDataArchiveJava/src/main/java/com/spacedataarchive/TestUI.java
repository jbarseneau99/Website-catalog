package com.spacedataarchive;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Simple test class to verify JavaFX is working correctly.
 */
public class TestUI extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        // Create a simple UI
        Label label = new Label("Test UI - JavaFX is working!");
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        Button button = new Button("Click Me");
        button.setOnAction(e -> {
            label.setText("Button clicked! JavaFX events are working!");
        });
        
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 20; -fx-alignment: center;");
        root.getChildren().addAll(label, button);
        
        Scene scene = new Scene(root, 600, 400);
        
        primaryStage.setTitle("JavaFX Test Window");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        System.out.println("JavaFX window should be visible now!");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 