package com.spacearchive;

import com.spacearchive.ui.MainApplication;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application entry point
 */
public class App {
    
    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        // Set look and feel to system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }
        
        // Enable URL diagnostics if specified in environment
        String enableUrlDiagnostics = System.getenv("ENABLE_URL_DIAGNOSTICS");
        if (enableUrlDiagnostics != null && enableUrlDiagnostics.equalsIgnoreCase("true")) {
            System.out.println("URL Diagnostics functionality enabled");
            System.setProperty("url.diagnostics.enabled", "true");
        }
        
        // Launch the application
        SwingUtilities.invokeLater(() -> {
            MainApplication app = new MainApplication();
            app.setVisible(true);
        });
    }
} 