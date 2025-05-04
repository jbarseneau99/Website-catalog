package com.spacedataarchive.service;

import com.spacedataarchive.monitoring.ClientConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for tracking client connections to the Space Data Archive System.
 * This service can track connections both in the local application and
 * query microservices for client connection information.
 */
public class ClientConnectionTrackingService {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionTrackingService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Connection trackers
    private final Map<String, ClientTracker> clientTrackers = new ConcurrentHashMap<>();
    private final AtomicLong nextClientId = new AtomicLong(1);
    
    // Microservice endpoints to query for connection information
    private static final String[] SERVICE_ENDPOINTS = {
        "http://localhost:8080/connections", // API Gateway
        "http://localhost:8081/connections", // URL Validation Service
        "http://localhost:8082/connections", // Catalog Processor
        "http://localhost:8083/connections"  // NLP Service
    };
    
    // Service names
    private static final String[] SERVICE_NAMES = {
        "API Gateway",
        "URL Validation Service",
        "Catalog Processor",
        "NLP Service"
    };
    
    /**
     * Register a new client connection.
     * 
     * @param clientIp The client's IP address
     * @param serviceName The service the client is connecting to
     * @return The client ID
     */
    public String registerClient(String clientIp, String serviceName) {
        String clientId = "client-" + nextClientId.getAndIncrement();
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        
        ClientTracker tracker = new ClientTracker(
                clientId, 
                clientIp, 
                serviceName, 
                timestamp, 
                ClientConnectionStatus.ConnectionState.ACTIVE);
        
        clientTrackers.put(clientId, tracker);
        logger.info("Registered new client: {}", tracker);
        
        return clientId;
    }
    
    /**
     * Update a client's last activity.
     * 
     * @param clientId The client ID
     */
    public void updateClientActivity(String clientId) {
        ClientTracker tracker = clientTrackers.get(clientId);
        if (tracker != null) {
            tracker.updateLastActivity();
            logger.debug("Updated client activity: {}", clientId);
        } else {
            logger.warn("Attempted to update non-existent client: {}", clientId);
        }
    }
    
    /**
     * Update a client's connection state.
     * 
     * @param clientId The client ID
     * @param state The new connection state
     */
    public void updateClientState(String clientId, ClientConnectionStatus.ConnectionState state) {
        ClientTracker tracker = clientTrackers.get(clientId);
        if (tracker != null) {
            tracker.setState(state);
            logger.debug("Updated client state: {} -> {}", clientId, state);
        } else {
            logger.warn("Attempted to update non-existent client: {}", clientId);
        }
    }
    
    /**
     * Remove a client connection (mark as disconnected).
     * 
     * @param clientId The client ID
     */
    public void removeClient(String clientId) {
        ClientTracker tracker = clientTrackers.get(clientId);
        if (tracker != null) {
            tracker.setState(ClientConnectionStatus.ConnectionState.DISCONNECTED);
            logger.info("Client disconnected: {}", clientId);
        } else {
            logger.warn("Attempted to remove non-existent client: {}", clientId);
        }
    }
    
    /**
     * Gets a list of client connections, combining local and microservice connections.
     * 
     * @return List of client connection statuses
     */
    public List<ClientConnectionStatus> getAllClientConnections() {
        logger.info("Retrieving all client connection information");
        List<ClientConnectionStatus> connections = new ArrayList<>();
        
        // First add local connections
        for (ClientTracker tracker : clientTrackers.values()) {
            connections.add(tracker.toClientConnectionStatus());
        }
        
        // Then attempt to query connections from microservices
        try {
            List<ClientConnectionStatus> microserviceConnections = getMicroserviceConnections();
            connections.addAll(microserviceConnections);
        } catch (Exception e) {
            logger.warn("Failed to retrieve connections from microservices: {}", e.getMessage());
            // Do not add simulated data
        }
        
        return connections;
    }
    
    /**
     * Query microservices for client connections.
     * Now uses real HTTP requests to /connections endpoints.
     *
     * @return List of client connections from microservices
     */
    private List<ClientConnectionStatus> getMicroserviceConnections() {
        List<ClientConnectionStatus> connections = new ArrayList<>();
        for (int i = 0; i < SERVICE_ENDPOINTS.length; i++) {
            String endpoint = SERVICE_ENDPOINTS[i];
            String serviceName = SERVICE_NAMES[i];
            try {
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    // Parse JSON array of IPs
                    JSONArray arr = new JSONArray(response.toString());
                    for (int j = 0; j < arr.length(); j++) {
                        String clientIp = arr.getString(j);
                        connections.add(new ClientConnectionStatus(
                            "client-" + clientIp, clientIp, serviceName,
                            "N/A", ClientConnectionStatus.ConnectionState.ACTIVE, "N/A"
                        ));
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to retrieve connections from {}: {}", endpoint, e.getMessage());
                // If a service is unreachable, skip it (do not add simulated data)
            }
        }
        return connections;
    }
    
    /**
     * Internal class for tracking client connections.
     */
    private class ClientTracker {
        private final String clientId;
        private final String clientIp;
        private final String serviceName;
        private final String connectedSince;
        private String lastActivity;
        private ClientConnectionStatus.ConnectionState state;
        
        /**
         * Create a new client tracker.
         */
        public ClientTracker(String clientId, String clientIp, String serviceName, 
                String connectedSince, ClientConnectionStatus.ConnectionState state) {
            this.clientId = clientId;
            this.clientIp = clientIp;
            this.serviceName = serviceName;
            this.connectedSince = connectedSince;
            this.lastActivity = connectedSince;
            this.state = state;
        }
        
        /**
         * Update the last activity timestamp.
         */
        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now().format(TIME_FORMATTER);
            if (this.state == ClientConnectionStatus.ConnectionState.IDLE) {
                this.state = ClientConnectionStatus.ConnectionState.ACTIVE;
            }
        }
        
        /**
         * Set the connection state.
         */
        public void setState(ClientConnectionStatus.ConnectionState state) {
            this.state = state;
            
            // If activating, update last activity
            if (state == ClientConnectionStatus.ConnectionState.ACTIVE) {
                updateLastActivity();
            }
        }
        
        /**
         * Convert to a ClientConnectionStatus object.
         */
        public ClientConnectionStatus toClientConnectionStatus() {
            return new ClientConnectionStatus(
                    clientId, clientIp, serviceName, connectedSince, state, lastActivity);
        }
        
        @Override
        public String toString() {
            return "ClientTracker{" +
                    "clientId='" + clientId + '\'' +
                    ", clientIp='" + clientIp + '\'' +
                    ", serviceName='" + serviceName + '\'' +
                    ", state=" + state +
                    '}';
        }
    }
} 