package com.spacedataarchive.monitoring;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Pane that displays Kubernetes resources and their statuses.
 */
public class KubernetesResourcesPane extends VBox {
    private static final Logger logger = LoggerFactory.getLogger(KubernetesResourcesPane.class);
    
    private TableView<KubernetesResource> resourcesTable;
    private ObservableList<KubernetesResource> resources = FXCollections.observableArrayList();
    
    /**
     * Constructs a new KubernetesResourcesPane.
     */
    public KubernetesResourcesPane() {
        setPadding(new Insets(10));
        setSpacing(10);
        
        Label title = new Label("Kubernetes Resources");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        resourcesTable = createResourcesTable();
        VBox.setVgrow(resourcesTable, Priority.ALWAYS);
        
        // Add Claude AI service as a specialized resource
        addClaudeAIService();
        
        getChildren().addAll(title, resourcesTable);
    }
    
    /**
     * Creates the resources table.
     * 
     * @return The created table
     */
    private TableView<KubernetesResource> createResourcesTable() {
        TableView<KubernetesResource> table = new TableView<>();
        
        TableColumn<KubernetesResource, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<KubernetesResource, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(100);
        
        TableColumn<KubernetesResource, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);
        
        TableColumn<KubernetesResource, String> ageColumn = new TableColumn<>("Age");
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        ageColumn.setPrefWidth(80);
        
        TableColumn<KubernetesResource, String> containerColumn = new TableColumn<>("Container");
        containerColumn.setCellValueFactory(new PropertyValueFactory<>("container"));
        containerColumn.setPrefWidth(120);
        
        table.getColumns().addAll(nameColumn, typeColumn, statusColumn, ageColumn, containerColumn);
        table.setItems(resources);
        
        return table;
    }
    
    /**
     * Adds Claude AI service to the resources.
     */
    private void addClaudeAIService() {
        // Create a custom resource for Claude AI that shows it's contained within the NLP service
        KubernetesResource claude = new KubernetesResource(
                "claude-ai", 
                "AI Model", 
                "Active", 
                "5m",
                "nlp-service"
        );
        resources.add(claude);
    }
    
    /**
     * Updates the resource data.
     * 
     * @param newResources The updated resources
     */
    public void updateResources(List<KubernetesResource> newResources) {
        resources.clear();
        
        // Add Claude AI service first
        addClaudeAIService();
        
        // Add the rest of the resources
        resources.addAll(newResources);
    }
} 