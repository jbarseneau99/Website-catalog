package com.spacedataarchive.portmanager.controller;

import org.springframework.web.bind.annotation.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/port")
public class PortController {
    private final ConcurrentHashMap<String, Integer> servicePorts = new ConcurrentHashMap<>();
    private final AtomicInteger nextPort = new AtomicInteger(8100); // Start from 8100

    @GetMapping("/assign/{serviceName}")
    public int assignPort(@PathVariable String serviceName) {
        return servicePorts.computeIfAbsent(serviceName, k -> nextPort.getAndIncrement());
    }

    @GetMapping("/get/{serviceName}")
    public Integer getPort(@PathVariable String serviceName) {
        return servicePorts.get(serviceName);
    }

    @GetMapping("/list")
    public ConcurrentHashMap<String, Integer> listPorts() {
        return servicePorts;
    }
} 