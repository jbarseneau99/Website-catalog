package com.spacedataarchive.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service for interacting with Anthropic's Claude AI.
 */
public class AnthropicAIService {
    private static final Logger logger = LoggerFactory.getLogger(AnthropicAIService.class);
    private static final String DEFAULT_MODEL = "claude-3-7-sonnet-20250219";
    private static final String CONFIG_FILE = "config/anthropic.properties";
    private static final String ANTHROPIC_API_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_API_VERSION = "2023-06-01";
    
    private String model;
    private Properties config;
    private boolean isInitialized = false;
    private String apiKey;
    
    /**
     * Constructor that initializes the Anthropic client.
     */
    public AnthropicAIService() {
        loadConfiguration();
        loadApiKeyFromEnv();
        initializeClient();
    }
    
    /**
     * Loads configuration from the properties file.
     */
    private void loadConfiguration() {
        config = new Properties();
        Path configPath = Paths.get(CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try {
                config.load(Files.newInputStream(configPath));
                logger.info("Loaded Anthropic configuration from {}", CONFIG_FILE);
                
                // Trim property values to avoid whitespace issues
                for (String key : config.stringPropertyNames()) {
                    String value = config.getProperty(key);
                    if (value != null) {
                        config.setProperty(key, value.trim());
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to load Anthropic configuration", e);
            }
        } else {
            logger.warn("Anthropic configuration file not found: {}", CONFIG_FILE);
            // Create a default configuration file
            try {
                Files.createDirectories(configPath.getParent());
                config.setProperty("api.key", "YOUR_ANTHROPIC_API_KEY");
                config.setProperty("model", DEFAULT_MODEL);
                config.store(Files.newOutputStream(configPath), "Anthropic AI Configuration");
                logger.info("Created default Anthropic configuration at {}", CONFIG_FILE);
            } catch (IOException e) {
                logger.error("Failed to create default Anthropic configuration", e);
            }
        }
        
        model = config.getProperty("model", DEFAULT_MODEL);
    }
    
    /**
     * Loads API key from environment variables or env.txt file
     */
    private void loadApiKeyFromEnv() {
        apiKey = getApiKey();
        logger.info("Using API key from environment variable");
        
        // Print the key length and first few characters to verify
        if (apiKey != null) {
            logger.info("API key loaded - length: {}, starts with: {}", 
                apiKey.length(), 
                apiKey.length() > 10 ? apiKey.substring(0, 10) : "too short");
        }
    }
    
    private String getApiKey() {
        return System.getenv("ANTHROPIC_API_KEY");
    }
    
    /**
     * Initializes the Anthropic client.
     */
    private void initializeClient() {
        if (apiKey == null || apiKey.isEmpty() || "YOUR_ANTHROPIC_API_KEY".equals(apiKey)) {
            logger.warn("Anthropic API key not configured. AI functionality will be limited.");
            isInitialized = false;
            return;
        }
        
        // Log the first few characters of the API key for debugging (don't log the whole key for security)
        logger.info("Using Anthropic API key starting with: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        logger.info("API key length: {}", apiKey.length());
        
        // Remove any whitespace or newlines that might have been accidentally added
        apiKey = apiKey.trim();
        
        // Test connection to Anthropic API
        try {
            URL url = new URI(ANTHROPIC_API_ENDPOINT).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", ANTHROPIC_API_VERSION);
            
            // Log the headers for debugging
            logger.debug("Request headers: Content-Type: application/json, x-api-key: [REDACTED], anthropic-version: {}", ANTHROPIC_API_VERSION);
            
            connection.setDoOutput(true);
            
            // Create a minimal request to test connection
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1);
            
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "test");
            messages.put(userMessage);
            requestBody.put("messages", messages);
            
            String jsonInputString = requestBody.toString();
            logger.debug("Test connection request body: {}", jsonInputString);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            logger.info("Test connection response code: {}", responseCode);
            
            if (responseCode == 200) {
                isInitialized = true;
                logger.info("Anthropic AI client initialized successfully with model: {}", model);
                
                // Read and log the successful response for debugging
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    logger.debug("Test connection successful response: {}", response.toString());
                }
            } else {
                // Log detailed error info
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    
                    String errorMsg = errorResponse.toString();
                    logger.error("Failed to connect to Anthropic API: HTTP {} - {}", 
                        responseCode, errorMsg);
                    
                    // Try to parse the error message for more details
                    try {
                        JSONObject errorJson = new JSONObject(errorMsg);
                        if (errorJson.has("error") && errorJson.getJSONObject("error").has("message")) {
                            String specificError = errorJson.getJSONObject("error").getString("message");
                            logger.error("Anthropic API error: {}", specificError);
                        }
                    } catch (Exception e) {
                        logger.error("Could not parse error response", e);
                    }
                    
                    isInitialized = false;
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Anthropic AI client", e);
            isInitialized = false;
        }
    }
    
    /**
     * Sends a message to Claude and receives a response.
     * 
     * @param prompt The user's prompt
     * @return The AI response
     */
    public String sendMessage(String prompt) {
        if (!isInitialized) {
            return "AI service is not properly configured. Please check your API key in " + CONFIG_FILE;
        }
        
        logger.info("Sending message to Anthropic API with model {}", DEFAULT_MODEL);
        
        try {
            // Make sure API key is clean
            apiKey = apiKey.trim();
            
            URL url = new URI(ANTHROPIC_API_ENDPOINT).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("x-api-key", apiKey);
            connection.setRequestProperty("anthropic-version", ANTHROPIC_API_VERSION);
            connection.setDoOutput(true);
            
            // Log request headers for debugging
            logger.debug("Request headers: Content-Type: application/json, x-api-key: [REDACTED], anthropic-version: {}", ANTHROPIC_API_VERSION);
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("max_tokens", 1024);
            
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            requestBody.put("messages", messages);
            
            // Set system prompt
            requestBody.put("system", "You are Claude, an AI assistant created by Anthropic to be helpful, harmless, and honest.");
            
            String jsonInputString = requestBody.toString();
            logger.debug("Request body: {}", jsonInputString);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            logger.info("Response code from Anthropic API: {}", responseCode);
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    
                    String responseStr = response.toString();
                    logger.debug("Response: {}", responseStr);
                    
                    JSONObject jsonResponse = new JSONObject(responseStr);
                    if (jsonResponse.has("content") && jsonResponse.getJSONArray("content").length() > 0) {
                        JSONArray contentArray = jsonResponse.getJSONArray("content");
                        StringBuilder fullResponse = new StringBuilder();
                        
                        for (int i = 0; i < contentArray.length(); i++) {
                            JSONObject contentItem = contentArray.getJSONObject(i);
                            if (contentItem.getString("type").equals("text")) {
                                fullResponse.append(contentItem.getString("text"));
                            }
                        }
                        
                        return fullResponse.toString();
                    } else {
                        return "Received empty response from Anthropic API";
                    }
                }
            } else {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    StringBuilder errorResponse = new StringBuilder();
                    
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    
                    String errorStr = errorResponse.toString();
                    logger.error("Failed to get response from Anthropic API: HTTP {} - {}", 
                        responseCode, errorStr);
                    
                    // Try to extract error message from JSON response
                    try {
                        JSONObject errorJson = new JSONObject(errorStr);
                        if (errorJson.has("error") && errorJson.getJSONObject("error").has("message")) {
                            String apiErrorMessage = errorJson.getJSONObject("error").getString("message");
                            logger.error("API Error message: {}", apiErrorMessage);
                            return "API Error: " + apiErrorMessage;
                        }
                    } catch (Exception e) {
                        // Ignore JSON parsing errors in error message
                        logger.error("Could not parse error response JSON", e);
                    }
                    
                    return "Error communicating with AI service (HTTP " + responseCode + ")";
                }
            }
        } catch (Exception e) {
            logger.error("Error communicating with Anthropic API", e);
            return "Error communicating with AI service: " + e.getMessage();
        }
    }
    
    /**
     * Sends a message to Claude with a streaming response.
     * 
     * @param prompt The user's prompt
     * @param responseHandler Handler for the streaming response
     */
    public void sendMessageStreaming(String prompt, Consumer<String> responseHandler) {
        if (!isInitialized) {
            responseHandler.accept("AI service is not properly configured. Please check your API key in " + CONFIG_FILE);
            return;
        }
        
        logger.info("Sending streaming message to Anthropic API with model {}", model);
        
        new Thread(() -> {
            try {
                URL url = new URI(ANTHROPIC_API_ENDPOINT).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("x-api-key", apiKey);
                connection.setRequestProperty("anthropic-version", ANTHROPIC_API_VERSION);
                connection.setDoOutput(true);
                
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", model);
                requestBody.put("max_tokens", 1024);
                requestBody.put("stream", true);
                
                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.put(userMessage);
                requestBody.put("messages", messages);
                
                // Set system prompt
                requestBody.put("system", "You are Claude, an AI assistant created by Anthropic to be helpful, harmless, and honest.");
                
                String jsonInputString = requestBody.toString();
                logger.debug("Streaming request body: {}", jsonInputString);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6).trim();
                            if (!jsonData.equals("[DONE]")) {
                                try {
                                    JSONObject eventData = new JSONObject(jsonData);
                                    if (eventData.has("delta") && 
                                        eventData.getJSONObject("delta").has("text")) {
                                        String textChunk = eventData.getJSONObject("delta").getString("text");
                                        responseHandler.accept(textChunk);
                                    } else if (eventData.has("type") && eventData.getString("type").equals("content_block_delta")) {
                                        if (eventData.has("delta") && eventData.getJSONObject("delta").has("text")) {
                                            String textChunk = eventData.getJSONObject("delta").getString("text");
                                            responseHandler.accept(textChunk);
                                        }
                                    } else {
                                        logger.debug("Received non-text event: {}", jsonData);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Error parsing streaming response chunk: {}", e.getMessage());
                                }
                            }
                        }
                    }
                    in.close();
                } else {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                        String errorLine;
                        StringBuilder errorResponse = new StringBuilder();
                        
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        
                        String errorStr = errorResponse.toString();
                        logger.error("Failed to get streaming response from Anthropic API: HTTP {} - {}", 
                            responseCode, errorStr);
                        
                        // Try to extract error message from JSON response
                        try {
                            JSONObject errorJson = new JSONObject(errorStr);
                            if (errorJson.has("error") && errorJson.getJSONObject("error").has("message")) {
                                responseHandler.accept("API Error: " + errorJson.getJSONObject("error").getString("message"));
                                return;
                            }
                        } catch (Exception e) {
                            // Ignore JSON parsing errors in error message
                        }
                        
                        responseHandler.accept("Error communicating with AI service (HTTP " + responseCode + ")");
                    }
                }
            } catch (Exception e) {
                logger.error("Error streaming response from Anthropic API", e);
                responseHandler.accept("Error communicating with AI service: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Updates the API key.
     * 
     * @param apiKey The new API key
     * @return True if the key was updated successfully
     */
    public boolean updateApiKey(String apiKey) {
        try {
            this.apiKey = apiKey;
            config.setProperty("api.key", apiKey);
            config.store(Files.newOutputStream(Paths.get(CONFIG_FILE)), "Anthropic AI Configuration");
            initializeClient();
            return isInitialized;
        } catch (IOException e) {
            logger.error("Failed to save API key", e);
            return false;
        }
    }
    
    /**
     * Updates the model.
     * 
     * @param newModel The new model to use
     * @return True if the model was updated successfully
     */
    public boolean updateModel(String newModel) {
        try {
            model = newModel;
            config.setProperty("model", newModel);
            config.store(Files.newOutputStream(Paths.get(CONFIG_FILE)), "Anthropic AI Configuration");
            return true;
        } catch (IOException e) {
            logger.error("Failed to save model configuration", e);
            return false;
        }
    }
    
    /**
     * Checks if the service is properly initialized.
     * 
     * @return True if the service is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Gets the current model.
     * 
     * @return The current model
     */
    public String getModel() {
        return model;
    }
    
    /**
     * Gets the available models.
     * 
     * @return List of available models
     */
    public List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();
        models.add("claude-3-opus-20240229");
        models.add("claude-3-sonnet-20240229");
        models.add("claude-3-haiku-20240307");
        return models;
    }
} 