package com.spacedataarchive.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * URL Validation Service Application.
 * This service provides URL validation and health checking capabilities.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
public class UrlValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlValidationServiceApplication.class, args);
    }
} 