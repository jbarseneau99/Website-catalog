package com.spacedataarchive.nlp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NLP Service Application.
 * This service provides natural language processing capabilities.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NlpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NlpServiceApplication.class, args);
    }
} 