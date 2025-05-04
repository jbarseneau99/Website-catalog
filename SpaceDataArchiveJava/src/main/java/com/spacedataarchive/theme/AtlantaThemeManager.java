package com.spacedataarchive.theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import atlantafx.base.theme.*;
import javafx.application.Application;

/**
 * Manages Atlanta-based themes for the application.
 */
public class AtlantaThemeManager {
    private static final Logger logger = LoggerFactory.getLogger(AtlantaThemeManager.class);
    
    // Theme constants
    public enum ThemeVariant {
        PRIMER_LIGHT, PRIMER_DARK, NORD_LIGHT, NORD_DARK, CUPERTINO_LIGHT, CUPERTINO_DARK, DRACULA
    }
    
    private static AtlantaThemeManager instance;
    private ThemeVariant currentTheme = ThemeVariant.PRIMER_LIGHT;
    
    private AtlantaThemeManager() {
        // Private constructor for singleton pattern
        logger.info("Initializing AtlantaThemeManager");
    }
    
    /**
     * Get the singleton instance
     * @return AtlantaThemeManager instance
     */
    public static synchronized AtlantaThemeManager getInstance() {
        if (instance == null) {
            instance = new AtlantaThemeManager();
        }
        return instance;
    }
    
    /**
     * Apply the Atlanta theme to the application
     * @param variant The theme variant to apply
     */
    public void applyTheme(ThemeVariant variant) {
        this.currentTheme = variant;
        
        // Create and apply the selected theme
        switch (variant) {
            case PRIMER_LIGHT:
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                logger.info("Applied Atlanta Primer Light theme");
                break;
            case PRIMER_DARK:
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                logger.info("Applied Atlanta Primer Dark theme");
                break;
            case NORD_LIGHT:
                Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
                logger.info("Applied Atlanta Nord Light theme");
                break;
            case NORD_DARK:
                Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
                logger.info("Applied Atlanta Nord Dark theme");
                break;
            case CUPERTINO_LIGHT:
                Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
                logger.info("Applied Atlanta Cupertino Light theme");
                break;
            case CUPERTINO_DARK:
                Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
                logger.info("Applied Atlanta Cupertino Dark theme");
                break;
            case DRACULA:
                Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
                logger.info("Applied Atlanta Dracula theme");
                break;
            default:
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                logger.info("Applied default Atlanta Primer Light theme");
                break;
        }
    }
    
    /**
     * Toggle between light and dark themes within the same family
     */
    public void toggleLightDark() {
        ThemeVariant newTheme;
        
        switch (currentTheme) {
            case PRIMER_LIGHT:
                newTheme = ThemeVariant.PRIMER_DARK;
                break;
            case PRIMER_DARK:
                newTheme = ThemeVariant.PRIMER_LIGHT;
                break;
            case NORD_LIGHT:
                newTheme = ThemeVariant.NORD_DARK;
                break;
            case NORD_DARK:
                newTheme = ThemeVariant.NORD_LIGHT;
                break;
            case CUPERTINO_LIGHT:
                newTheme = ThemeVariant.CUPERTINO_DARK;
                break;
            case CUPERTINO_DARK:
                newTheme = ThemeVariant.CUPERTINO_LIGHT;
                break;
            case DRACULA:
                // Dracula is dark only, so no toggle
                newTheme = ThemeVariant.DRACULA;
                break;
            default:
                newTheme = ThemeVariant.PRIMER_LIGHT;
                break;
        }
        
        applyTheme(newTheme);
    }
    
    /**
     * Get the current theme variant
     * @return Current ThemeVariant
     */
    public ThemeVariant getCurrentTheme() {
        return currentTheme;
    }
    
    /**
     * Get a human-readable name for the current theme
     * @return Name of the current theme
     */
    public String getThemeName() {
        switch (currentTheme) {
            case PRIMER_LIGHT:
                return "Atlanta Primer Light";
            case PRIMER_DARK:
                return "Atlanta Primer Dark";
            case NORD_LIGHT:
                return "Atlanta Nord Light";
            case NORD_DARK:
                return "Atlanta Nord Dark";
            case CUPERTINO_LIGHT:
                return "Atlanta Cupertino Light";
            case CUPERTINO_DARK:
                return "Atlanta Cupertino Dark";
            case DRACULA:
                return "Atlanta Dracula";
            default:
                return "Atlanta Theme";
        }
    }
    
    /**
     * Check if the current theme is a dark variant
     * @return true if the current theme is dark
     */
    public boolean isDarkTheme() {
        return currentTheme == ThemeVariant.PRIMER_DARK ||
               currentTheme == ThemeVariant.NORD_DARK ||
               currentTheme == ThemeVariant.CUPERTINO_DARK ||
               currentTheme == ThemeVariant.DRACULA;
    }
} 