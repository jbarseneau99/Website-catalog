#!/bin/bash

# Source common functions
source "$(dirname "$0")/common-functions.sh"

# Validate service structure
validate_service() {
    local service_dir=$1
    local service_name=$(basename "$service_dir")
    local errors=0
    
    print_info "Validating service: $service_name"
    
    # Check required files
    local required_files=(
        "pom.xml"
        "src/main/resources/application.yml"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "${service_dir}/${file}" ]; then
            print_error "Missing ${file} in ${service_name}"
            errors=$((errors + 1))
        fi
    done
    
    # Check required directories
    local required_dirs=(
        "src/main/java"
        "src/main/resources"
        "src/test/java"
    )
    
    for dir in "${required_dirs[@]}"; do
        if [ ! -d "${service_dir}/${dir}" ]; then
            print_error "Missing directory: ${dir} in ${service_name}"
            errors=$((errors + 1))
        fi
    done
    
    # Check main application class
    if ! find "${service_dir}/src/main/java" -name "*Application.java" | grep -q .; then
        print_error "Missing main application class in ${service_name}"
        errors=$((errors + 1))
    fi
    
    # Check CORS configuration in application.yml
    if [ -f "${service_dir}/src/main/resources/application.yml" ]; then
        if ! grep -q "cors:" "${service_dir}/src/main/resources/application.yml"; then
            print_error "Missing CORS configuration in ${service_name}"
            errors=$((errors + 1))
        fi
    fi
    
    # Check health check endpoint configuration
    if [ -f "${service_dir}/src/main/resources/application.yml" ]; then
        if ! grep -q "management.endpoints.web.exposure.include: \"*\"" "${service_dir}/src/main/resources/application.yml"; then
            print_error "Missing health check configuration in ${service_name}"
            errors=$((errors + 1))
        fi
    fi
    
    return $errors
}

# Create service structure
create_service_structure() {
    local service_dir=$1
    local service_name=$(basename "$service_dir")
    local package_path="src/main/java/com/spacedataarchive/${service_name}"
    
    print_info "Creating service structure for: ${service_name}"
    
    # Create directories
    mkdir -p "${service_dir}/${package_path}"
    mkdir -p "${service_dir}/src/main/resources"
    mkdir -p "${service_dir}/src/test/java"
    
    # Create main application class if it doesn't exist
    local main_class="${service_dir}/${package_path}/${service_name^}Application.java"
    if [ ! -f "$main_class" ]; then
        cat > "$main_class" << EOF
package com.spacedataarchive.${service_name};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ${service_name^}Application {
    public static void main(String[] args) {
        SpringApplication.run(${service_name^}Application.class, args);
    }
}
EOF
    fi
    
    # Create application.yml if it doesn't exist
    local config_file="${service_dir}/src/main/resources/application.yml"
    if [ ! -f "$config_file" ]; then
        cat > "$config_file" << EOF
spring:
  application:
    name: ${service_name}

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    registerWithEureka: true
    fetchRegistry: true
  instance:
    preferIpAddress: true

management:
  endpoints:
    web:
      exposure:
        include: "*"
      cors:
        allowed-origins: "http://localhost:3000"
        allowed-methods: GET,POST,PUT,DELETE,OPTIONS
        allowed-headers: "*"
        allow-credentials: true
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.spacedataarchive: DEBUG
    org.springframework.web.cors: DEBUG
EOF
    fi
    
    # Create pom.xml if it doesn't exist
    if [ ! -f "${service_dir}/pom.xml" ]; then
        cat > "${service_dir}/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.spacedataarchive</groupId>
        <artifactId>space-data-archive-microservices</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>${service_name}</artifactId>
    <name>${service_name^} Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
EOF
    fi
}

# Main execution
main() {
    local fix_issues=false
    
    # Parse command line options
    while getopts "f" opt; do
        case $opt in
            f)
                fix_issues=true
                ;;
            \?)
                print_error "Invalid option: -$OPTARG"
                exit 1
                ;;
        esac
    done
    
    # Validate core services
    local core_services=("service-discovery" "api-gateway")
    for service in "${core_services[@]}"; do
        if [ ! -d "$service" ]; then
            print_error "Missing core service: $service"
            if [ "$fix_issues" = true ]; then
                create_service_structure "$service"
            fi
        else
            validate_service "$service"
        fi
    done
    
    # Validate other services
    for service_dir in services/*/; do
        if [ -d "$service_dir" ]; then
            if ! validate_service "$service_dir"; then
                if [ "$fix_issues" = true ]; then
                    create_service_structure "$service_dir"
                fi
            fi
        fi
    done
}

# Execute main function
main "$@" 