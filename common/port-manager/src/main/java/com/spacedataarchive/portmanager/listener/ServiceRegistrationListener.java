package com.spacedataarchive.portmanager.listener;

import com.spacedataarchive.portmanager.service.PortManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceRegistrationListener {
    
    private final PortManagerService portManager;

    @Autowired
    public ServiceRegistrationListener(PortManagerService portManager) {
        this.portManager = portManager;
    }

    @EventListener
    public void onServiceRegistered(EurekaInstanceRegisteredEvent event) {
        String serviceId = event.getInstanceInfo().getAppName();
        try {
            int port = portManager.assignPort(serviceId);
            log.info("Assigned port {} to newly registered service {}", port, serviceId);
        } catch (Exception e) {
            log.error("Failed to assign port for service {}: {}", serviceId, e.getMessage());
        }
    }

    @EventListener
    public void onServiceDeregistered(EurekaInstanceCanceledEvent event) {
        String serviceId = event.getAppName();
        try {
            portManager.releasePort(serviceId);
            log.info("Released port for deregistered service {}", serviceId);
        } catch (Exception e) {
            log.error("Failed to release port for service {}: {}", serviceId, e.getMessage());
        }
    }
} 