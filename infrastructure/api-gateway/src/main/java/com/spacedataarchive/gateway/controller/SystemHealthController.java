package com.spacedataarchive.gateway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> getSystemHealth() {
        Map<String, String> healthStatus = new HashMap<>();
        
        // Check Eureka Server
        healthStatus.put("eureka", discoveryClient.getServices().contains("EUREKA-SERVER") ? "UP" : "DOWN");
        
        // Check URL Validation Service
        healthStatus.put("urlValidation", discoveryClient.getServices().contains("URL-VALIDATION-SERVICE") ? "UP" : "DOWN");
        
        // Check Catalog Processor
        healthStatus.put("catalogProcessor", discoveryClient.getServices().contains("CATALOG-PROCESSOR") ? "UP" : "DOWN");
        
        // Check NLP Service
        healthStatus.put("nlpService", discoveryClient.getServices().contains("NLP-SERVICE") ? "UP" : "DOWN");
        
        // Check MongoDB (if any service is up, MongoDB is up)
        boolean isMongoUp = discoveryClient.getServices().stream()
            .anyMatch(service -> service.equals("URL-VALIDATION-SERVICE") || 
                               service.equals("CATALOG-PROCESSOR") || 
                               service.equals("NLP-SERVICE"));
        healthStatus.put("mongodb", isMongoUp ? "UP" : "DOWN");
        
        return ResponseEntity.ok(healthStatus);
    }
} 