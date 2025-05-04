package com.spacearchive.ui;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Main application class for the Space Data Archive System
 */
public class MainApplication extends JFrame {
    
    private static final Logger LOGGER = Logger.getLogger(MainApplication.class.getName());
    private JTabbedPane tabbedPane;
    private UrlDiagnosticsPanel urlDiagnosticsPanel;
    
    public MainApplication() {
        LOGGER.info("Initializing Main Application");
        initUI();
    }
    
    private void initUI() {
        setTitle("Space Data Archive System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, 
            "Space Data Archive System\nVersion 1.0", "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        // Add UI library name to the right side of the menu bar
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel uiLibraryLabel = new JLabel("Built with Swing UI");
        uiLibraryLabel.setHorizontalAlignment(JLabel.RIGHT);
        uiLibraryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        uiLibraryLabel.setForeground(new Color(100, 100, 100));
        uiLibraryLabel.setFont(new Font(uiLibraryLabel.getFont().getName(), Font.ITALIC, 12));
        headerPanel.add(menuBar, BorderLayout.CENTER);
        headerPanel.add(uiLibraryLabel, BorderLayout.EAST);
        
        setJMenuBar(menuBar);
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create placeholder panel with label for most tabs
        tabbedPane.addTab("Site Map Creation", createPlaceholderPanel("Site Map Creation functionality"));
        tabbedPane.addTab("URL Validation", createPlaceholderPanel("URL Validation functionality"));
        tabbedPane.addTab("Logs & Diagnostics", createPlaceholderPanel("Logs & Diagnostics functionality"));
        
        // Create URL Diagnostics panel
        LOGGER.info("Creating URL Diagnostics Panel");
        urlDiagnosticsPanel = new UrlDiagnosticsPanel();
        tabbedPane.addTab("URL Diagnostics", urlDiagnosticsPanel);
        
        // Add remaining tabs
        tabbedPane.addTab("Data Extraction", createPlaceholderPanel("Data Extraction functionality"));
        tabbedPane.addTab("Analysis", createPlaceholderPanel("Analysis functionality"));
        tabbedPane.addTab("Search & Access", createPlaceholderPanel("Search & Access functionality"));
        tabbedPane.addTab("Catalog", createPlaceholderPanel("Catalog functionality"));
        tabbedPane.addTab("Intelligent Agents", createPlaceholderPanel("Intelligent Agents functionality"));
        tabbedPane.addTab("AI Registry", createPlaceholderPanel("AI Registry functionality"));
        
        // Add header panel with the library name and title
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Space Data Archive System");
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
        
        JLabel uiLibraryHeaderLabel = new JLabel("Powered by Java Swing");
        uiLibraryHeaderLabel.setHorizontalAlignment(JLabel.RIGHT);
        uiLibraryHeaderLabel.setFont(new Font(uiLibraryHeaderLabel.getFont().getName(), Font.ITALIC, 12));
        uiLibraryHeaderLabel.setForeground(new Color(80, 80, 80));
        uiLibraryHeaderLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(uiLibraryHeaderLabel, BorderLayout.EAST);
        titlePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        
        topPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Add status bar
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        statusPanel.setLayout(new BorderLayout());
        JLabel statusLabel = new JLabel("Application ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        // Create AI Assistant panel
        JPanel aiAssistantPanel = new JPanel();
        aiAssistantPanel.setBackground(new Color(52, 152, 219));
        aiAssistantPanel.setPreferredSize(new Dimension(getWidth(), 30));
        JLabel aiAssistantLabel = new JLabel("AI Assistant");
        aiAssistantLabel.setForeground(Color.WHITE);
        aiAssistantPanel.add(aiAssistantLabel);
        
        // Add components to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(aiAssistantPanel, BorderLayout.SOUTH);
        
        // Select the URL Diagnostics tab by default to make it visible
        tabbedPane.setSelectedIndex(3);
        
        // Set position
        setLocationRelativeTo(null);
        
        LOGGER.info("UI initialization complete");
    }
    
    /**
     * Create a placeholder panel with centered label
     */
    private JPanel createPlaceholderPanel(String labelText) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setFont(new Font(label.getFont().getName(), Font.BOLD, 16));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
    
    public static void main(String[] args) {
        // Set look and feel to system
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create and show application
        SwingUtilities.invokeLater(() -> {
            LOGGER.info("Starting application");
            MainApplication app = new MainApplication();
            app.setVisible(true);
        });
    }
} 