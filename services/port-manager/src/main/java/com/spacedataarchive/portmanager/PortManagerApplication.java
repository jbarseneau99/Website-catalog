package com.spacedataarchive.portmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class PortManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortManagerApplication.class, args);
    }
} 