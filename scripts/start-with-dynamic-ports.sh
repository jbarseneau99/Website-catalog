#!/bin/bash

echo "🚀 Starting services with dynamic port allocation..."

# Function to get a dynamic port from Port Manager
get_dynamic_port() {
    local service_name=$1
    local default_port=$2
    local response
    
    # Try to get a port from Port Manager
    response=$(curl -s "http://localhost:8090/api/ports/assign/$service_name")
    
    if [ $? -eq 0 ] && [ ! -z "$response" ]; then
        echo "$response"
    else
        echo "$default_port"
    fi
}

# Stop any running containers
echo "🔄 Stopping existing containers..."
docker-compose down

# Start core services first
echo "🌟 Starting core services..."
docker-compose up -d mongodb eureka-server port-manager

# Wait for core services to be healthy
echo "⏳ Waiting for core services..."
sleep 30

# Get dynamic ports for each service
echo "🎯 Assigning dynamic ports..."
export PORT_API_GATEWAY=$(get_dynamic_port "API-GATEWAY" 8080)
export PORT_URL_VALIDATION=$(get_dynamic_port "URL-VALIDATION" 8081)
export PORT_NLP_SERVICE=$(get_dynamic_port "NLP-SERVICE" 8082)
export PORT_CATALOG_PROCESSOR=$(get_dynamic_port "CATALOG-PROCESSOR" 8083)

# Start application services with dynamic ports
echo "🚀 Starting application services..."
docker-compose up -d api-gateway url-validation nlp-service catalog-processor

# Wait for services to start
echo "⏳ Waiting for services to start..."
sleep 30

# Display service information
echo -e "\n📊 Service Status:"
echo "----------------"
echo "Eureka Server: http://localhost:8761"
echo "API Gateway: http://localhost:$PORT_API_GATEWAY"
echo "URL Validation: http://localhost:$PORT_URL_VALIDATION"
echo "NLP Service: http://localhost:$PORT_NLP_SERVICE"
echo "Catalog Processor: http://localhost:$PORT_CATALOG_PROCESSOR"
echo "Port Manager: http://localhost:8090"
echo "MongoDB: localhost:27017"

# Show Docker container status
echo -e "\n🐳 Container Status:"
docker-compose ps

echo -e "\n✅ All services started successfully!" 