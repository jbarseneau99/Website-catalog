#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Function to initialize Maven wrapper
init_mvn_wrapper() {
    local service_dir=$1
    echo -e "${YELLOW}Initializing Maven wrapper in ${service_dir}${NC}"
    cd "$service_dir"
    mvn -N wrapper:wrapper
    cd ..
}

# Initialize Maven wrapper in all service directories
echo -e "${YELLOW}Initializing Maven wrappers for all services...${NC}"

# Service Discovery
init_mvn_wrapper "service-discovery"

# API Gateway
init_mvn_wrapper "api-gateway"

# Individual services
cd services
for service in */; do
    if [ -d "$service" ]; then
        cd "$service"
        mvn -N wrapper:wrapper
        cd ..
    fi
done
cd ..

# Common library
cd common
mvn -N wrapper:wrapper
cd ..

echo -e "${GREEN}Maven wrappers initialized successfully!${NC}" 