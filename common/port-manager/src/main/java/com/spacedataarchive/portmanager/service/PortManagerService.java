package com.spacedataarchive.portmanager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Service
public class PortManagerService {
    private final Map<String, Integer> servicePortMap = new ConcurrentHashMap<>();
    private final Set<Integer> reservedPorts = new ConcurrentSkipListSet<>();
    
    // Port ranges for different service types
    private static final int MIN_PORT = 8080;
    private static final int MAX_PORT = 8999;
    private static final Set<Integer> EXCLUDED_PORTS = Set.of(8761); // Eureka Server port
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @PostConstruct
    public void init() {
        // Initialize with any existing port assignments
        log.info("Initializing Port Manager Service");
    }

    public synchronized int assignPort(String serviceId) {
        if (servicePortMap.containsKey(serviceId)) {
            return servicePortMap.get(serviceId);
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            int port = findAvailablePort();
            if (port != -1) {
                try {
                    if (isPortStable(port)) {
                        servicePortMap.put(serviceId, port);
                        reservedPorts.add(port);
                        log.info("Assigned port {} to service {} on attempt {}", port, serviceId, attempt + 1);
                        return port;
                    }
                } catch (Exception e) {
                    log.warn("Failed to assign port {} on attempt {}: {}", port, attempt + 1, e.getMessage());
                    if (attempt < MAX_RETRIES - 1) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        throw new RuntimeException("No available ports found for service " + serviceId + " after " + MAX_RETRIES + " attempts");
    }

    public synchronized void releasePort(String serviceId) {
        Integer port = servicePortMap.remove(serviceId);
        if (port != null) {
            reservedPorts.remove(port);
            cleanupPort(port);
            log.info("Released port {} from service {}", port, serviceId);
        }
    }

    private int findAvailablePort() {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            if (isPortAvailable(port) && !EXCLUDED_PORTS.contains(port)) {
                return port;
            }
        }
        return -1;
    }

    private boolean isPortAvailable(int port) {
        if (reservedPorts.contains(port)) {
            return false;
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isPortStable(int port) {
        // Check port stability by attempting to bind multiple times
        for (int i = 0; i < 3; i++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                Thread.sleep(100); // Short delay between checks
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void cleanupPort(int port) {
        try {
            // For Unix-like systems
            Process process = Runtime.getRuntime().exec("lsof -ti:" + port);
            process.waitFor();
            
            if (process.exitValue() == 0) {
                // Port is in use, kill the process
                Runtime.getRuntime().exec("kill -9 $(lsof -ti:" + port + ")");
                log.info("Cleaned up process using port {}", port);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to cleanup port {}: {}", port, e.getMessage());
        }
    }

    public Set<Integer> getReservedPorts() {
        return new ConcurrentSkipListSet<>(reservedPorts);
    }
} 