#!/bin/bash

echo "Building all microservices..."
mvn clean install -DskipTests

echo "Starting Service Discovery..."
cd service-discovery
mvn spring-boot:run &
EUREKA_PID=$!
cd ..

echo "Waiting for Eureka server to start..."
sleep 20

echo "Starting API Gateway..."
cd api-gateway
mvn spring-boot:run &
GATEWAY_PID=$!
cd ..

echo "Waiting for API Gateway to start..."
sleep 15

echo "Starting URL Validation Service..."
cd services/url-validation
mvn spring-boot:run &
VALIDATION_PID=$!
cd ../..

echo "Starting Catalog Processor Service..."
cd services/catalog-processor
mvn spring-boot:run &
CATALOG_PID=$!
cd ../..

echo "Starting NLP Service..."
cd services/nlp-service
mvn spring-boot:run &
NLP_PID=$!
cd ../..

echo "All services started!"
echo "Service Discovery: http://localhost:8761"
echo "API Gateway: http://localhost:8080"
echo "URL Validation: http://localhost:8081"
echo "Catalog Processor: http://localhost:8082"
echo "NLP Service: http://localhost:8083"

echo "Press Ctrl+C to stop all services..."
trap "kill $EUREKA_PID $GATEWAY_PID $VALIDATION_PID $CATALOG_PID $NLP_PID; exit" INT
wait 