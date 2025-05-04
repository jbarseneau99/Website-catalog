package com.spacearchive.diagnostics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Client for communicating with the URL diagnostic service
 */
public class UrlDiagnosticClient {
    private final String serviceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new client with the default service URL
     */
    public UrlDiagnosticClient() {
        this("http://localhost:3000");
    }
    
    /**
     * Creates a new client with a custom service URL
     * 
     * @param serviceUrl Base URL of the diagnostic service
     */
    public UrlDiagnosticClient(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Check if the diagnostic service is available
     * 
     * @return true if the service is available, false otherwise
     */
    public boolean isServiceAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error checking service availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Analyze a URL using the diagnostic service
     * 
     * @param url URL to analyze
     * @return Diagnostic result
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public DiagnosticResult analyzeUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/analyze-url"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString("{\"url\":\"" + url + "\"}"))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Error analyzing URL. Status code: " + response.statusCode());
        }
        
        return objectMapper.readValue(response.body(), DiagnosticResult.class);
    }
    
    /**
     * Get the list of recently analyzed URLs
     * 
     * @return List of recently analyzed URLs
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    public List<String> getRecentUrls() throws IOException, InterruptedException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl + "/recent-urls"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }
            
            // Parse the JSON array of strings
            String[] urls = objectMapper.readValue(response.body(), String[].class);
            return Arrays.asList(urls);
        } catch (Exception e) {
            System.err.println("Error getting recent URLs: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Analyze multiple URLs
     * 
     * @param urls List of URLs to analyze
     * @return List of diagnostic results
     */
    public CompletableFuture<List<DiagnosticResult>> analyzeUrls(List<String> urls) {
        List<CompletableFuture<DiagnosticResult>> futures = new ArrayList<>();
        
        for (String url : urls) {
            CompletableFuture<DiagnosticResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return analyzeUrl(url);
                } catch (Exception e) {
                    DiagnosticResult result = new DiagnosticResult(url, -1);
                    return result;
                }
            });
            
            futures.add(future);
        }
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<DiagnosticResult> results = new ArrayList<>();
                    for (CompletableFuture<DiagnosticResult> future : futures) {
                        try {
                            results.add(future.get());
                        } catch (Exception e) {
                            // Skip failed results
                        }
                    }
                    return results;
                });
    }
    
    /**
     * Analyze multiple URLs
     * 
     * @param urls List of URLs to analyze
     * @param outputFile Optional output file name
     * @return SummaryResult containing summary statistics
     * @throws IOException If an IO error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public SummaryResult analyzeUrls(List<String> urls, String outputFile) throws IOException, InterruptedException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode urlsNode = requestBody.putArray("urls");
        urls.forEach(urlsNode::add);
        
        if (outputFile != null && !outputFile.isEmpty()) {
            requestBody.put("outputFile", outputFile);
        }
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/analyze-urls"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(120))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Error analyzing URLs: " + response.body());
        }
        
        return parseSummaryResult(response.body());
    }
    
    /**
     * Load URLs from a file
     * 
     * @param filePath Path to the file containing URLs
     * @return List of URLs loaded from the file
     * @throws IOException If an IO error occurs
     * @throws InterruptedException If the request is interrupted
     */
    public List<String> loadUrlsFromFile(String filePath) throws IOException, InterruptedException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("filePath", filePath);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl + "/load-urls"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Error loading URLs from file: " + response.body());
        }
        
        List<String> urls = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(response.body());
        
        if (rootNode.has("urls") && rootNode.get("urls").isArray()) {
            for (JsonNode urlNode : rootNode.get("urls")) {
                urls.add(urlNode.asText());
            }
        }
        
        return urls;
    }
    
    /**
     * Parse the summary result JSON
     */
    private SummaryResult parseSummaryResult(String json) throws IOException {
        JsonNode rootNode = objectMapper.readTree(json);
        
        SummaryResult result = new SummaryResult();
        result.setTotal(getIntValue(rootNode, "total"));
        result.setSuccess(getIntValue(rootNode, "success"));
        result.setRedirects(getIntValue(rootNode, "redirects"));
        result.setErrors(getIntValue(rootNode, "errors"));
        result.setEmptyContent(getIntValue(rootNode, "emptyContent"));
        result.setNoAssets(getIntValue(rootNode, "noAssets"));
        result.setWithAssets(getIntValue(rootNode, "withAssets"));
        
        return result;
    }
    
    // Helper method to safely get values from JSON
    private int getIntValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asInt() : 0;
    }
} 