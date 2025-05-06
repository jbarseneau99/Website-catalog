#!/bin/bash

# Set environment variables
export SPRING_PROFILES_ACTIVE=staging
export MONGODB_URI="mongodb+srv://jbarseneau:Cheech99@cluster0.rd3fpku.mongodb.net/website-catalog-staging"
export PORT_MANAGER_URL="http://port-manager:8090"
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Function to wait for container health
wait_for_container() {
    local container=$1
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $container to be healthy..."
    while [ $attempt -le $max_attempts ]; do
        if docker inspect "$container" --format='{{.State.Health.Status}}' 2>/dev/null | grep -q "healthy"; then
            echo "$container is healthy"
            return 0
        fi
        echo "Attempt $attempt: $container not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "$container failed to become healthy"
    return 1
}

# Clean up existing containers
echo "Cleaning up existing containers..."
docker-compose down -v
docker system prune -f

# Build all services
echo "Building all services..."
./mvnw clean package -DskipTests

# Build and start containers
echo "Building and starting containers..."
docker-compose -f docker-compose.yml build
docker-compose -f docker-compose.yml up -d

# Wait for core services
wait_for_container "eureka-server"
wait_for_container "port-manager"

# Show container status
echo "Container Status:"
docker-compose ps

# Show service logs
echo "Service Logs:"
docker-compose logs -f 