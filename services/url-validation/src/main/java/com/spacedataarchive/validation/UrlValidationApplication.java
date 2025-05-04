package com.spacedataarchive.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
public class UrlValidationApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlValidationApplication.class, args);
    }
} 