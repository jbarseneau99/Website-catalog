package com.spacedataarchive.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for monitoring the health of microservices.
 */
public class ServiceMonitoringService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceMonitoringService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Service endpoints
    private static final String[] SERVICE_ENDPOINTS = {
        "http://localhost:8761", // Eureka Server
        "http://localhost:8080", // API Gateway
        "http://localhost:8081", // URL Validation Service
        "http://localhost:8082", // Catalog Processor
        "http://localhost:8083"  // NLP Service
    };
    
    // Service names
    private static final String[] SERVICE_NAMES = {
        "Eureka Server",
        "API Gateway",
        "URL Validation Service",
        "Catalog Processor",
        "NLP Service"
    };
    
    // Connection timeout in seconds
    private static final int CONNECTION_TIMEOUT = 3;
    
    /**
     * Checks all services and returns their health status.
     */
    public List<ServiceHealthStatus> checkAllServices() {
        logger.info("Checking health status of all services");
        List<ServiceHealthStatus> statuses = new ArrayList<>();
        
        for (int i = 0; i < SERVICE_ENDPOINTS.length; i++) {
            String serviceName = SERVICE_NAMES[i];
            String endpoint = SERVICE_ENDPOINTS[i];
            
            // Extract port from endpoint
            int port = extractPortFromEndpoint(endpoint);
            
            ServiceHealthStatus.Status status = checkServiceHealth(endpoint);
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            
            ServiceHealthStatus serviceStatus = new ServiceHealthStatus(
                    serviceName, status, timestamp, port);
            
            statuses.add(serviceStatus);
            logger.debug("Service {} health status: {}", serviceName, status);
        }
        
        return statuses;
    }
    
    /**
     * Extracts port number from an endpoint URL.
     * 
     * @param endpoint The endpoint URL
     * @return The port number, or -1 if none found
     */
    private int extractPortFromEndpoint(String endpoint) {
        Pattern pattern = Pattern.compile(":(\\d+)(?:/|$)");
        Matcher matcher = pattern.matcher(endpoint);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1; // Default port
    }
    
    /**
     * Checks the health of a specific service.
     */
    private ServiceHealthStatus.Status checkServiceHealth(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(CONNECTION_TIMEOUT));
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            if (responseCode >= 200 && responseCode < 300) {
                return ServiceHealthStatus.Status.HEALTHY;
            } else if (responseCode >= 400 && responseCode < 500) {
                return ServiceHealthStatus.Status.DEGRADED;
            } else {
                return ServiceHealthStatus.Status.DOWN;
            }
        } catch (IOException e) {
            logger.warn("Failed to connect to service at {}: {}", endpoint, e.getMessage());
            return ServiceHealthStatus.Status.DOWN;
        }
    }
    
    /**
     * Gets a list of Kubernetes resources.
     */
    public List<KubernetesResource> getKubernetesResources() {
        logger.info("Fetching Kubernetes resources");
        List<KubernetesResource> resources = new ArrayList<>();
        
        // In a real implementation, this would query the Kubernetes API
        // For now, just return some placeholder values
        
        resources.add(new KubernetesResource("eureka-server", "Pod", "Running", "10m"));
        resources.add(new KubernetesResource("api-gateway", "Pod", "Running", "10m"));
        resources.add(new KubernetesResource("url-validation", "Pod", "Running", "10m"));
        resources.add(new KubernetesResource("catalog-processor", "Pod", "Running", "10m"));
        resources.add(new KubernetesResource("nlp-service", "Pod", "Running", "10m"));
        resources.add(new KubernetesResource("service-discovery", "Service", "Active", "10m"));
        resources.add(new KubernetesResource("mongodb", "StatefulSet", "Running", "10m"));
        
        // Add Claude AI model as a specialized resource contained within the NLP service
        resources.add(new KubernetesResource("claude-3.7-sonnet", "AI Model", "Active", "5m", "nlp-service"));
        resources.add(new KubernetesResource("claude-instant", "AI Model", "Standby", "5m", "nlp-service"));
        
        return resources;
    }
    
    /**
     * Gets a list of active client connections.
     * 
     * @return List of client connection statuses
     */
    public List<ClientConnectionStatus> getClientConnections() {
        logger.info("Retrieving client connection information");
        List<ClientConnectionStatus> connections = new ArrayList<>();
        
        // This is a simulated implementation
        // In a real system, you would query your services for active client connections
        
        // Add some sample client connections for demonstration
        // In a real system, this data would come from your services
        
        // Add sample connections to the API Gateway
        connections.add(new ClientConnectionStatus(
                "client-001", "192.168.1.101", "API Gateway",
                LocalDateTime.now().minusMinutes(5).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.ACTIVE,
                LocalDateTime.now().format(TIME_FORMATTER)));
        
        connections.add(new ClientConnectionStatus(
                "client-002", "192.168.1.102", "API Gateway",
                LocalDateTime.now().minusMinutes(10).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.ACTIVE,
                LocalDateTime.now().minusSeconds(30).format(TIME_FORMATTER)));
        
        // Add sample connections to the NLP Service
        connections.add(new ClientConnectionStatus(
                "client-003", "192.168.1.105", "NLP Service",
                LocalDateTime.now().minusMinutes(15).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.IDLE,
                LocalDateTime.now().minusMinutes(2).format(TIME_FORMATTER)));
        
        // Add sample connection to the Catalog Processor
        connections.add(new ClientConnectionStatus(
                "client-004", "192.168.1.110", "Catalog Processor",
                LocalDateTime.now().minusMinutes(20).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.ACTIVE,
                LocalDateTime.now().minusSeconds(45).format(TIME_FORMATTER)));
        
        // Add a couple of recently disconnected clients
        connections.add(new ClientConnectionStatus(
                "client-005", "192.168.1.115", "URL Validation Service",
                LocalDateTime.now().minusMinutes(30).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.DISCONNECTED,
                LocalDateTime.now().minusMinutes(3).format(TIME_FORMATTER)));
                
        // Add a client with error state
        connections.add(new ClientConnectionStatus(
                "client-006", "192.168.1.120", "Eureka Server",
                LocalDateTime.now().minusMinutes(25).format(TIME_FORMATTER),
                ClientConnectionStatus.ConnectionState.ERROR,
                LocalDateTime.now().minusMinutes(5).format(TIME_FORMATTER)));
        
        return connections;
    }
} 