package com.spacedataarchive.portmanager.monitor;

import com.spacedataarchive.portmanager.service.PortManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class PortMonitor {
    
    private final PortManagerService portManager;

    @Autowired
    public PortMonitor(PortManagerService portManager) {
        this.portManager = portManager;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    public void checkOrphanedPorts() {
        log.debug("Checking for orphaned ports...");
        Set<Integer> reservedPorts = portManager.getReservedPorts();
        
        for (Integer port : reservedPorts) {
            if (!isPortInUse(port)) {
                log.info("Found orphaned port: {}", port);
                // Find service ID by port and release it
                releaseOrphanedPort(port);
            }
        }
    }

    private boolean isPortInUse(int port) {
        try {
            Process process = Runtime.getRuntime().exec("lsof -i:" + port);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            log.error("Error checking port {}: {}", port, e.getMessage());
            return false;
        }
    }

    private void releaseOrphanedPort(int port) {
        try {
            // Kill any process using this port
            Process killProcess = Runtime.getRuntime().exec("kill -9 $(lsof -ti:" + port + ")");
            killProcess.waitFor();
            log.info("Cleaned up orphaned process on port {}", port);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to cleanup orphaned port {}: {}", port, e.getMessage());
        }
    }
} 