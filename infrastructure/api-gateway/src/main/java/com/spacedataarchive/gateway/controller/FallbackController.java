package com.spacedataarchive.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @GetMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just("{\"status\": \"Service temporarily unavailable\", \"message\": \"Please try again later\"}");
    }
} 