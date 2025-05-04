package com.spacedataarchive.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Service for handling application logging.
 */
public class LogService {
    @SuppressWarnings("unused") // Will be used in future implementation
    private Consumer<String> logConsumer;
    private UILogAppender uiAppender;
    
    /**
     * Sets the log level for the application.
     * 
     * @param level The log level to set
     */
    public void setLogLevel(String level) {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        
        switch (level) {
            case "DEBUG":
                root.setLevel(Level.DEBUG);
                break;
            case "INFO":
                root.setLevel(Level.INFO);
                break;
            case "WARN":
                root.setLevel(Level.WARN);
                break;
            case "ERROR":
                root.setLevel(Level.ERROR);
                break;
            default:
                root.setLevel(Level.INFO);
        }
    }
    
    /**
     * Sets the consumer that will receive log messages.
     * 
     * @param consumer The log consumer
     */
    public void setLogConsumer(Consumer<String> consumer) {
        this.logConsumer = consumer;
        
        // Remove existing appender if any
        if (uiAppender != null) {
            Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.detachAppender(uiAppender);
            uiAppender.stop();
        }
        
        // Create and add a new appender
        uiAppender = new UILogAppender(consumer);
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(uiAppender);
        uiAppender.start();
    }
    
    /**
     * Exports logs to a file.
     * 
     * @param file The file to export to
     * @param content The content to export
     * @throws IOException If an I/O error occurs
     */
    public void exportLogs(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
    
    /**
     * Custom Logback appender that forwards logs to a UI consumer.
     */
    private static class UILogAppender extends AppenderBase<ILoggingEvent> {
        private final Consumer<String> logConsumer;
        
        public UILogAppender(Consumer<String> logConsumer) {
            this.logConsumer = logConsumer;
            setContext((LoggerContext) LoggerFactory.getILoggerFactory());
            setName("UILogAppender");
        }
        
        @Override
        protected void append(ILoggingEvent event) {
            if (logConsumer != null) {
                // Format the log message with timestamp and level
                String formattedMessage = String.format("[%s] %s - %s", 
                    event.getLevel().toString(),
                    event.getLoggerName(),
                    event.getFormattedMessage());
                
                logConsumer.accept(formattedMessage);
            }
        }
    }
} 