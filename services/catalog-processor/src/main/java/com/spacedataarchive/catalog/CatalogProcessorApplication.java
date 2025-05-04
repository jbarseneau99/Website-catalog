package com.spacedataarchive.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Catalog Processor Service Application.
 * This service processes and manages space data catalogs.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories
public class CatalogProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogProcessorApplication.class, args);
    }
} 