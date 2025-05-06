#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to start a service
start_service() {
    local service_name=$1
    local jar_path=$2
    local port=$3
    
    echo -e "${YELLOW}Starting $service_name on port $port...${NC}"
    
    # Check if port is already in use
    if lsof -i :$port > /dev/null; then
        echo -e "${RED}Port $port is already in use. Please free the port first.${NC}"
        return 1
    }
    
    # Start the service
    java -jar $jar_path --server.port=$port &
    
    # Wait for service to start
    for i in {1..30}; do
        if curl -s http://localhost:$port/actuator/health > /dev/null; then
            echo -e "${GREEN}$service_name started successfully on port $port${NC}"
            return 0
        fi
        sleep 1
    done
    
    echo -e "${RED}Failed to start $service_name${NC}"
    return 1
}

# Kill any existing Java processes (optional, uncomment if needed)
# pkill -f java

# Start infrastructure services first
echo -e "${YELLOW}Starting infrastructure services...${NC}"
start_service "Service Discovery" "infrastructure/service-discovery/target/service-discovery.jar" 8761
sleep 5  # Wait for Eureka to start

start_service "API Gateway" "infrastructure/api-gateway/target/api-gateway.jar" 8080
sleep 3

# Start business services
echo -e "${YELLOW}Starting business services...${NC}"
start_service "Catalog Processor" "services/catalog-processor/target/catalog-processor.jar" 8083
start_service "NLP Service" "services/nlp-service/target/nlp-service.jar" 8082
start_service "URL Validation" "services/url-validation/target/url-validation.jar" 8081
start_service "Port Manager" "services/port-manager/target/port-manager.jar" 8090

# Start observability services
echo -e "${YELLOW}Starting observability services...${NC}"
start_service "Prometheus" "infrastructure/observability/prometheus/prometheus.jar" 9090
start_service "Jaeger" "infrastructure/observability/jaeger/jaeger-all-in-one.jar" 16686

echo -e "${GREEN}All services started. Service discovery is available at http://localhost:8761${NC}"
echo -e "${GREEN}API Gateway is available at http://localhost:8080${NC}"
echo -e "${GREEN}Prometheus is available at http://localhost:9090${NC}"
echo -e "${GREEN}Jaeger is available at http://localhost:16686${NC}"

# Keep script running
wait 