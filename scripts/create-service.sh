#!/bin/bash

# Usage: ./scripts/create-service.sh "Service Name"

if [ -z "$1" ]; then
    echo "❌ Please provide a service name"
    echo "Usage: ./scripts/create-service.sh \"Service Name\""
    exit 1
fi

# Convert service name to different formats
ORIGINAL_NAME="$1"
SERVICE_NAME=$(echo "$ORIGINAL_NAME" | tr '[:lower:]' '[:upper:]' | tr ' ' '-')
PACKAGE_NAME=$(echo "$ORIGINAL_NAME" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')
JAVA_PACKAGE_NAME=$(echo "$ORIGINAL_NAME" | tr '[:upper:]' '[:lower:]' | tr ' ' '.' | tr '-' '.')

echo "🚀 Creating new microservice: $ORIGINAL_NAME"

# Create service directory structure
SERVICE_DIR="services/$PACKAGE_NAME"
mkdir -p "$SERVICE_DIR"/{src/main/{java/com/spacedataarchive/$PACKAGE_NAME,resources},src/test/java}

# Generate pom.xml
cat > "$SERVICE_DIR/pom.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.spacedataarchive</groupId>
        <artifactId>services</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>$PACKAGE_NAME</artifactId>
    <name>$ORIGINAL_NAME</name>

    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <!-- Common Dependencies -->
        <dependency>
            <groupId>com.spacedataarchive</groupId>
            <artifactId>common</artifactId>
            <version>\${project.version}</version>
        </dependency>
    </dependencies>
</project>
EOF

# Generate application.yml
cat > "$SERVICE_DIR/src/main/resources/application.yml" << EOF
server:
  port: \${PORT:0}  # Dynamic port assignment

spring:
  application:
    name: $SERVICE_NAME
  data:
    mongodb:
      uri: \${MONGODB_URI:mongodb://localhost:27017/website-catalog}

eureka:
  client:
    serviceUrl:
      defaultZone: \${EUREKA_URL:http://localhost:8761}/eureka/
    registerWithEureka: true
    fetchRegistry: true
  instance:
    preferIpAddress: true
    instanceId: \${spring.application.name}:\${spring.application.instance_id:\${random.value}}

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
EOF

# Generate main application class
MAIN_CLASS_DIR="$SERVICE_DIR/src/main/java/com/spacedataarchive/$PACKAGE_NAME"
MAIN_CLASS_NAME=$(echo "$ORIGINAL_NAME" | sed -r 's/(^|-)(\w)/\U\2/g')

cat > "$MAIN_CLASS_DIR/${MAIN_CLASS_NAME}Application.java" << EOF
package com.spacedataarchive.$PACKAGE_NAME;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ${MAIN_CLASS_NAME}Application {
    public static void main(String[] args) {
        SpringApplication.run(${MAIN_CLASS_NAME}Application.class, args);
    }
}
EOF

# Create Docker configuration
cat > "$SERVICE_DIR/Dockerfile" << EOF
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/$PACKAGE_NAME-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

echo "✅ Created new microservice: $ORIGINAL_NAME"
echo "📁 Location: $SERVICE_DIR"
echo "🔨 Next steps:"
echo "1. Add your service-specific code in $MAIN_CLASS_DIR"
echo "2. Build the service: mvn clean install -DskipTests"
echo "3. Start the service: java -jar $SERVICE_DIR/target/$PACKAGE_NAME-*.jar"
echo "4. Check Eureka dashboard: http://localhost:8761" 