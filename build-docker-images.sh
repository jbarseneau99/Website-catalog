#!/bin/bash

# Exit on error
set -e

echo "Building microservices Docker images..."

# First build the maven projects
echo "Building Maven projects..."
mvn clean package -DskipTests

# Build each Docker image
echo "Building Docker images..."

# Build Eureka Server
cd service-discovery
docker build -t spacedataarchive/eureka-server:latest .
cd ..

# Build API Gateway
cd api-gateway
docker build -t spacedataarchive/api-gateway:latest .
cd ..

# Build URL Validation Service 
cd services/url-validation
docker build -t spacedataarchive/url-validation:latest .
cd ../..

# Build NLP Service
cd services/nlp-service
docker build -t spacedataarchive/nlp-service:latest .
cd ../..

# Build Catalog Processor Service
cd services/catalog-processor
docker build -t spacedataarchive/catalog-processor:latest .
cd ../..

echo "All Docker images built successfully!"
echo "You can now run 'docker-compose up' to start all services locally." 