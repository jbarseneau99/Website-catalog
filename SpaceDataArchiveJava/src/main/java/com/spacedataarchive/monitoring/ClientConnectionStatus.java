package com.spacedataarchive.monitoring;

/**
 * Represents the status of a client connection to a service.
 */
public class ClientConnectionStatus {
    
    /**
     * Possible connection state values.
     */
    public enum ConnectionState {
        ACTIVE,     // Client is actively connected
        IDLE,       // Client is connected but idle
        DISCONNECTED, // Client has disconnected
        ERROR       // Connection has an error
    }
    
    private final String clientId;
    private final String clientIp;
    private final String connectedService;
    private final String connectedSince;
    private final ConnectionState connectionState;
    private final String lastActivity;
    
    /**
     * Constructs a new ClientConnectionStatus with the specified values.
     * 
     * @param clientId The ID of the client
     * @param clientIp The IP address of the client
     * @param connectedService The service the client is connected to
     * @param connectedSince When the client connected
     * @param connectionState The current connection state
     * @param lastActivity When the client last performed an activity
     */
    public ClientConnectionStatus(String clientId, String clientIp, String connectedService,
                                 String connectedSince, ConnectionState connectionState,
                                 String lastActivity) {
        this.clientId = clientId;
        this.clientIp = clientIp;
        this.connectedService = connectedService;
        this.connectedSince = connectedSince;
        this.connectionState = connectionState;
        this.lastActivity = lastActivity;
    }
    
    /**
     * Gets the client ID.
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Gets the client IP address.
     */
    public String getClientIp() {
        return clientIp;
    }
    
    /**
     * Gets the connected service name.
     */
    public String getConnectedService() {
        return connectedService;
    }
    
    /**
     * Gets the connection timestamp.
     */
    public String getConnectedSince() {
        return connectedSince;
    }
    
    /**
     * Gets the current connection state.
     */
    public ConnectionState getConnectionState() {
        return connectionState;
    }
    
    /**
     * Gets the last activity timestamp.
     */
    public String getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Returns a string representation of the client connection status.
     */
    @Override
    public String toString() {
        return "ClientConnectionStatus{" +
                "clientId='" + clientId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", connectedService='" + connectedService + '\'' +
                ", connectedSince='" + connectedSince + '\'' +
                ", connectionState=" + connectionState +
                ", lastActivity='" + lastActivity + '\'' +
                '}';
    }
} 