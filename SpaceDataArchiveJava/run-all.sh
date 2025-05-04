#!/bin/bash

# Start Eureka Server
echo "Starting Eureka Server..."
./run-eureka.sh &

# Wait for Eureka to start
sleep 10

# Start API Gateway
echo "Starting API Gateway..."
JAVA_OPTS="-DMACH33_NAME=Mach33_api-gateway"
./run-api-gateway.sh $JAVA_OPTS &

# Start URL Validation Service
echo "Starting URL Validation Service..."
JAVA_OPTS="-DMACH33_NAME=Mach33_url-validation"
./run-url-validation.sh $JAVA_OPTS &

# Start Catalog Processor
echo "Starting Catalog Processor..."
JAVA_OPTS="-DMACH33_NAME=Mach33_catalog-processor"
./run-catalog-processor.sh $JAVA_OPTS &

# Start NLP Service
echo "Starting NLP Service..."
JAVA_OPTS="-DMACH33_NAME=Mach33_nlp-service"
./run-nlp-service.sh $JAVA_OPTS &

echo "All services started!"
echo "Service Discovery: http://localhost:8761"
echo "API Gateway: http://localhost:8080"
echo "URL Validation: http://localhost:8081"
echo "Catalog Processor: http://localhost:8082"
echo "NLP Service: http://localhost:8083"
echo "Press Ctrl+C to stop all services..."

# Keep the script running
wait 