package com.spacedataarchive.validation;

import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClientConnectionTracker {
    private final Set<String> activeClients = ConcurrentHashMap.newKeySet();

    public void clientConnected(String clientId) {
        activeClients.add(clientId);
    }

    public void clientDisconnected(String clientId) {
        activeClients.remove(clientId);
    }

    public Set<String> getActiveClients() {
        return activeClients;
    }
} 