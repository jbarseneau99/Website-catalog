#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Stopping all microservices...${NC}"

# Function to get service port from Port Manager
get_service_port() {
    local service_name=$1
    local port=$(curl -s "http://localhost:8090/port/current/$service_name" | jq -r '.port')
    echo $port
}

# Function to stop service on a specific port
stop_service() {
    local service_name=$1
    local port=$2
    local pid=$(lsof -ti:$port)
    
    if [ ! -z "$pid" ]; then
        echo -e "${YELLOW}Stopping $service_name on port $port (PID: $pid)${NC}"
        kill -15 $pid
        sleep 2
        if ps -p $pid > /dev/null; then
            echo -e "${RED}Force killing $service_name${NC}"
            kill -9 $pid
        fi
        # Release port from Port Manager if it's a dynamic port service
        if [ "$service_name" != "Eureka Server" ] && [ "$service_name" != "Port Manager" ]; then
            curl -s -X DELETE "http://localhost:8090/port/release/$service_name"
        fi
        echo -e "${GREEN}$service_name stopped${NC}"
    else
        echo -e "${YELLOW}No process found on port $port${NC}"
    fi
}

# Get current ports for services
CATALOG_PORT=$(get_service_port "catalog-processor")
NLP_PORT=$(get_service_port "nlp-service")
URL_VALIDATION_PORT=$(get_service_port "url-validation")

# Stop all services in reverse order
stop_service "Catalog Processor" $CATALOG_PORT
stop_service "NLP Service" $NLP_PORT
stop_service "URL Validation Service" $URL_VALIDATION_PORT
stop_service "Port Manager" 8090
stop_service "Eureka Server" 8761

# Clean up any remaining Java processes
echo -e "${YELLOW}Cleaning up any remaining Java processes...${NC}"
pkill -f java

# Clean up ports
./cleanup-ports.sh

echo -e "${GREEN}All services have been stopped successfully!${NC}" 