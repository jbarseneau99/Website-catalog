package com.spacedataarchive.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ConnectionController {
    private final ClientConnectionTracker tracker;

    public ConnectionController(ClientConnectionTracker tracker) {
        this.tracker = tracker;
    }

    @GetMapping("/connections")
    public Set<String> getConnections() {
        return tracker.getActiveClients().stream()
            .map(ip -> "Mach33:" + ip)
            .collect(Collectors.toSet());
    }
} 