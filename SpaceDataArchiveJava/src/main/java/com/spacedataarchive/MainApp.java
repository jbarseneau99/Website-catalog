package com.spacedataarchive;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import com.spacedataarchive.service.ai.AIServiceManager;
import com.spacedataarchive.theme.AtlantaThemeManager;
import com.spacedataarchive.theme.AtlantaThemeManager.ThemeVariant;
import com.spacedataarchive.util.DataCleanupUtility;
import com.spacedataarchive.controller.CatalogProcessingController;
import com.spacedataarchive.service.ServiceFactory;

/**
 * Main application class for the Space Data Archive.
 * This class is responsible for loading the UI and initializing the application.
 */
public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    // Theme constants
    private static final String USE_ATLANTA_ENV = "USE_ATLANTA";
    private static final String ATLANTA_THEME_ENV = "ATLANTA_THEME";
    private static final String USE_SYSTEM_THEME_ENV = "USE_SYSTEM_THEME";
    
    private static final String JAVAFX_VERSION = System.getProperty("javafx.version");
    private static boolean isJavaFX24OrHigher = false;
    
    private Stage primaryStage;
    private BorderPane rootLayout;
    private StackPane loadingPane;

    /**
     * Application entry point.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            // Check JavaFX version
            if (JAVAFX_VERSION != null) {
                String[] versionParts = JAVAFX_VERSION.split("\\.");
                if (versionParts.length > 0) {
                    int majorVersion = Integer.parseInt(versionParts[0]);
                    isJavaFX24OrHigher = majorVersion >= 24;
                    logger.info("Running with JavaFX version: {} (JavaFX 24+ features: {})", 
                                JAVAFX_VERSION, isJavaFX24OrHigher);
                }
            }
            
            logger.info("Starting Space Data Archive application");
            launch(args);
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Space Data Archive System");
        
        // The professional logo path for the window icon
        final String WINDOW_ICON = "/images/33fg_Logo.png";
        
        // Fix icon loading - using correct path to available logo image
        try {
            Image appIcon = new Image(getClass().getResourceAsStream(WINDOW_ICON));
            if (appIcon != null && !appIcon.isError()) {
                this.primaryStage.getIcons().add(appIcon);
                logger.info("Application icon loaded successfully");
            } else {
                logger.warn("Could not load application icon - image error");
            }
        } catch (Exception e) {
            logger.warn("Could not load application icon: {}", e.getMessage());
            // Continue without the icon
        }
        
        // Check JavaFX version
        try {
            int javafxMajorVersion = Integer.parseInt(JAVAFX_VERSION.split("\\.")[0]);
            isJavaFX24OrHigher = javafxMajorVersion >= 24;
            logger.info("Detected JavaFX version: {}, isJavaFX24OrHigher: {}", JAVAFX_VERSION, isJavaFX24OrHigher);
        } catch (Exception e) {
            logger.warn("Could not parse JavaFX version: {}", JAVAFX_VERSION);
        }
        
        // Create loading screen
        initLoadingScreen();
        
        // Show the loading screen
        Scene loadingScene = new Scene(loadingPane, 600, 400);
        primaryStage.setScene(loadingScene);
        primaryStage.show();
        
        // Initialize the application in the background with proper threading
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000); // Simulate loading time
                initRootLayout();
                
                // We'll wait a bit to ensure the root layout theme is applied
                Thread.sleep(500);
                
                // Show main view on the JavaFX thread
                Platform.runLater(() -> {
                    try {
                        showMainView();
                    } catch (Exception e) {
                        logger.error("Error showing main view", e);
                        showErrorScreen(e);
                    }
                });
                
            } catch (Exception e) {
                logger.error("Error during application initialization", e);
                Platform.runLater(() -> showErrorScreen(e));
            }
        });
    }
    
    /**
     * Initialize the loading screen.
     */
    private void initLoadingScreen() {
        loadingPane = new StackPane();
        loadingPane.getStyleClass().add("background");
        loadingPane.setStyle("-fx-background-color: #1A1C1E;"); // Dark background for splash screen
        
        VBox loadingBox = new VBox(24);
        loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
        loadingBox.setPadding(new javafx.geometry.Insets(30));
        loadingBox.setMaxWidth(500);
        loadingBox.setMaxHeight(400);
        loadingBox.getStyleClass().add("card");
        loadingBox.setStyle("-fx-background-color: #FDFCFF; -fx-background-radius: 24px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5);");
        
        // Logo container
        StackPane logoContainer = new StackPane();
        logoContainer.setMinHeight(140);
        logoContainer.setPadding(new javafx.geometry.Insets(10));
        
        // Logo image
        ImageView logoImageView = new ImageView();
        try {
            Image logoImage = new Image(getClass().getResourceAsStream("/images/33fg_Logo.png"));
            logoImageView.setImage(logoImage);
            logoImageView.setFitHeight(120);
            logoImageView.setPreserveRatio(true);
            logoImageView.setSmooth(true);
            logoImageView.setCache(true);
            logoContainer.getChildren().add(logoImageView);
        } catch (Exception e) {
            logger.warn("Could not load logo for splash screen: {}", e.getMessage());
        }
        
        // Title label
        Label titleLabel = new Label("Space Data Archive System");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #006495;");
        titleLabel.setAlignment(javafx.geometry.Pos.CENTER);
        
        // Subtitle label
        Label subtitleLabel = new Label("Advanced Mission Data Management");
        subtitleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #43474E; -fx-font-style: italic;");
        
        // Progress indicator with custom styling
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(64, 64);
        progressIndicator.setStyle("-fx-progress-color: #006495;");
        
        // Loading message
        Label loadingLabel = new Label("Loading application components...");
        loadingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #43474E;");
        
        // Version info
        Label versionLabel = new Label("JavaFX " + JAVAFX_VERSION + " • Apple Silicon Optimized");
        versionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #73777F;");
        
        // Add all elements to the loading box
        loadingBox.getChildren().addAll(
            logoContainer,
            titleLabel,
            subtitleLabel,
            new javafx.scene.control.Separator(),
            progressIndicator,
            loadingLabel,
            versionLabel
        );
        
        // Add drop shadow effect to the loading box
        loadingPane.getChildren().add(loadingBox);
    }

    /**
     * Initializes the root layout.
     */
    private void initRootLayout() {
        try {
            FXMLLoader loader = new FXMLLoader();
            // Use a more straightforward path format
            URL rootLayoutUrl = getClass().getResource("/fxml/RootLayout.fxml");
            
            if (rootLayoutUrl == null) {
                logger.error("Could not find RootLayout.fxml file at /fxml/RootLayout.fxml");
                throw new IOException("RootLayout.fxml file not found in resources");
            }
            
            loader.setLocation(rootLayoutUrl);
            rootLayout = loader.load();
            
            // We need to apply the theme on the JavaFX application thread
            final BorderPane finalRootLayout = rootLayout;
            Platform.runLater(() -> {
                // Apply the selected theme
                applyTheme(finalRootLayout);
            });
            
            // Initialize AI service (if enabled)
            AIServiceManager.getInstance();
            
            // Start data cleanup utility
            DataCleanupUtility.cleanupLargeDataFiles();
            
            logger.info("Root layout initialized successfully");
        } catch (IOException e) {
            logger.error("Error loading root layout", e);
            throw new RuntimeException("Failed to load application layout", e);
        }
    }
    
    /**
     * Apply the Atlanta theme based on environment variables.
     */
    private void applyTheme(BorderPane root) {
        Scene scene = new Scene(root);
        
        // Add application default CSS first
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        
        // Add AI chat CSS
        scene.getStylesheets().add(getClass().getResource("/css/ai-chat.css").toExternalForm());
        
        // Apply Atlanta theme
        AtlantaThemeManager themeManager = AtlantaThemeManager.getInstance();
        
        // Check environment variable to see which theme to use
        boolean useAtlanta = getBooleanEnv(USE_ATLANTA_ENV, true);
        boolean useSystemTheme = getBooleanEnv(USE_SYSTEM_THEME_ENV, false);
        
        if (useAtlanta) {
            // Use Atlanta theme
            String atlantaThemeEnv = System.getProperty(ATLANTA_THEME_ENV, "PRIMER_LIGHT");
            try {
                ThemeVariant variant = ThemeVariant.valueOf(atlantaThemeEnv);
                themeManager.applyTheme(variant);
                logger.info("Applied Atlanta theme: {}", variant);
            } catch (IllegalArgumentException e) {
                // If the environment variable contains an invalid theme, use the default
                logger.warn("Invalid Atlanta theme: {}. Using default PRIMER_LIGHT.", atlantaThemeEnv);
                themeManager.applyTheme(ThemeVariant.PRIMER_LIGHT);
            }
        }
        
        // Set the scene on the primary stage
        primaryStage.setScene(scene);
        
        // Set window size from properties
        int windowWidth = getIntProperty("WINDOW_WIDTH", 1200);
        int windowHeight = getIntProperty("WINDOW_HEIGHT", 800);
        boolean windowCentered = getBooleanProperty("WINDOW_CENTERED", true);
        
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);
        
        if (windowCentered) {
            primaryStage.centerOnScreen();
        }
        
        logger.info("Theme applied successfully");
    }

    /**
     * Shows the main view inside the root layout.
     */
    private void showMainView() {
        try {
            // Load main view
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/fxml/MainView.fxml"));
            BorderPane mainView = loader.load();
            
            // Set main view in the center of root layout
            rootLayout.setCenter(mainView);
            
            // Check if window size parameters are provided
            int windowWidth = getIntProperty("WINDOW_WIDTH", 1200);
            int windowHeight = getIntProperty("WINDOW_HEIGHT", 800);
            boolean windowCentered = getBooleanProperty("WINDOW_CENTERED", true);
            
            // Set window size based on parameters
            primaryStage.setWidth(windowWidth);
            primaryStage.setHeight(windowHeight);
            
            if (windowCentered) {
                primaryStage.centerOnScreen();
            }
            
            logger.info("Window initialized with size: {}x{}", windowWidth, windowHeight);
            logger.info("Main view loaded successfully");
        } catch (IOException e) {
            logger.error("Error loading main view", e);
            showErrorScreen(e);
        }
    }
    
    /**
     * Shows an error screen with the exception details.
     */
    private void showErrorScreen(Exception e) {
        VBox errorPane = new VBox(20);
        errorPane.setAlignment(javafx.geometry.Pos.CENTER);
        errorPane.setPadding(new javafx.geometry.Insets(20));
        
        Label errorLabel = new Label("Error Starting Application");
        errorLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: red;");
        
        Label detailsLabel = new Label("Error: " + e.getMessage());
        
        javafx.scene.control.TextArea stackTraceArea = new javafx.scene.control.TextArea();
        stackTraceArea.setPrefRowCount(10);
        stackTraceArea.setEditable(false);
        
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            stackTrace.append(element.toString()).append("\n");
        }
        stackTraceArea.setText(stackTrace.toString());
        
        errorPane.getChildren().addAll(errorLabel, detailsLabel, stackTraceArea);
        
        Scene errorScene = new Scene(errorPane, 600, 400);
        primaryStage.setScene(errorScene);
    }
    
    /**
     * Helper method to convert environment variable to boolean.
     */
    private boolean getBooleanEnv(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Helper method to read boolean system property.
     */
    private boolean getBooleanProperty(String name, boolean defaultValue) {
        String value = System.getProperty(name);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Helper method to read integer system property.
     */
    private int getIntProperty(String name, int defaultValue) {
        String value = System.getProperty(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer format for property {}: {}", name, value);
            return defaultValue;
        }
    }
    
    /**
     * Check if the application is running on JavaFX 24 or higher.
     * @return true if running on JavaFX 24+
     */
    public static boolean isJavaFX24OrHigher() {
        return isJavaFX24OrHigher;
    }

    @Override
    public void stop() {
        logger.info("Application stopping...");
        
        try {
            // Shutdown services directly without trying to load controllers
            try {
                if (ServiceFactory.getInstance() != null) {
                    // Ask ServiceFactory to stop all services, including CatalogProcessingService
                    ServiceFactory.getInstance().shutdown();
                    logger.info("ServiceFactory and all services shut down successfully");
                }
            } catch (Exception e) {
                logger.warn("Error shutting down ServiceFactory: {}", e.getMessage());
            }
            
            // Allow a moment for resources to clean up properly
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
        
        logger.info("Application stopped");
    }
} 