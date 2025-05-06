#!/bin/bash

# Initialize port manager with default port assignments
echo "Initializing port manager..."

# Function to register a service with its default port
register_service() {
    local service=$1
    local port=$2
    echo "Registering $service on port $port..."
    curl -X POST "http://localhost:8090/api/ports/register" \
        -H "Content-Type: application/json" \
        -d "{\"serviceName\": \"$service\", \"preferredPort\": $port}" \
        --fail --silent --show-error || echo "Failed to register $service"
}

# Wait for port manager to be available
max_attempts=30
attempt=1
while ! curl -s "http://localhost:8090/actuator/health" > /dev/null; do
    if [ $attempt -gt $max_attempts ]; then
        echo "Port manager not available after $max_attempts attempts. Exiting."
        exit 1
    fi
    echo "Waiting for port manager to be available... (attempt $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

# Register services with their default ports
register_service "eureka" 8761
register_service "gateway" 8080
register_service "urlValidation" 8081
register_service "nlpService" 8082
register_service "catalogProcessor" 8083
register_service "portManager" 8090

echo "Port initialization complete." 