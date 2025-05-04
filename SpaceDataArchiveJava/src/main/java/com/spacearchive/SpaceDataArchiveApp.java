package com.spacearchive;

import com.spacearchive.ui.UrlDiagnosticsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Main application class for the Space Data Archive System
 */
public class SpaceDataArchiveApp {
    
    private static final Logger LOGGER = Logger.getLogger(SpaceDataArchiveApp.class.getName());
    
    private JFrame mainFrame;
    private JTabbedPane tabbedPane;
    private UrlDiagnosticsPanel urlDiagnosticsPanel;
    
    /**
     * Constructor
     */
    public SpaceDataArchiveApp() {
        initUI();
    }
    
    /**
     * Initialize the user interface
     */
    private void initUI() {
        mainFrame = new JFrame("Space Data Archive System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(900, 700);
        mainFrame.setLocationRelativeTo(null);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create main panels
        JPanel siteMapPanel = createSiteMapPanel();
        JPanel catalogPanel = createCatalogPanel();
        JPanel searchPanel = createSearchPanel();
        
        // Add URL Diagnostics panel if enabled
        boolean enableUrlDiagnostics = Boolean.parseBoolean(
                System.getProperty("ENABLE_URL_DIAGNOSTICS", 
                System.getenv("ENABLE_URL_DIAGNOSTICS")));
        
        if (enableUrlDiagnostics) {
            LOGGER.info("URL Diagnostics feature is enabled");
            urlDiagnosticsPanel = new UrlDiagnosticsPanel();
            tabbedPane.addTab("URL Diagnostics", urlDiagnosticsPanel);
        } else {
            LOGGER.info("URL Diagnostics feature is disabled");
        }
        
        // Add other panels
        tabbedPane.addTab("Site Map", siteMapPanel);
        tabbedPane.addTab("Catalog", catalogPanel);
        tabbedPane.addTab("Search & Access", searchPanel);
        
        mainFrame.getContentPane().add(tabbedPane);
        
        // Add menu bar
        JMenuBar menuBar = createMenuBar();
        mainFrame.setJMenuBar(menuBar);
    }
    
    /**
     * Create the Site Map panel
     */
    private JPanel createSiteMapPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Site Map Panel - Implementation coming soon"), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the Catalog panel
     */
    private JPanel createCatalogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Catalog Panel - Implementation coming soon"), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the Search & Access panel
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Search & Access Panel - Implementation coming soon"), BorderLayout.CENTER);
        return panel;
    }
    
    /**
     * Create the application menu bar
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        // Tools menu
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem diagnosticsItem = new JMenuItem("URL Diagnostics");
        diagnosticsItem.addActionListener(e -> {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).equals("URL Diagnostics")) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        });
        toolsMenu.add(diagnosticsItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }
    
    /**
     * Show the about dialog
     */
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(mainFrame,
                "Space Data Archive System\nVersion 1.0\n\n" +
                "A system for cataloging and accessing space-related data",
                "About Space Data Archive System",
                JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Show the application window
     */
    public void show() {
        mainFrame.setVisible(true);
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        // Set native look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.warning("Failed to set system look and feel: " + e.getMessage());
        }
        
        // Run on the EDT
        SwingUtilities.invokeLater(() -> {
            SpaceDataArchiveApp app = new SpaceDataArchiveApp();
            app.show();
        });
    }
} 