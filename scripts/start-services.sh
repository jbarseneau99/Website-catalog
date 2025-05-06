#!/bin/bash

# Start core services first
echo "Starting core services..."
docker-compose up -d mongodb eureka-server port-manager

# Wait for core services to be healthy
echo "Waiting for core services to be healthy..."
docker-compose run --rm port-manager /scripts/get-service-port.sh port-manager

# Start application services
echo "Starting application services..."
for service in api-gateway url-validation nlp-service catalog-processor; do
    echo "Starting $service..."
    # Get port from Port Manager
    PORT=$(docker-compose run --rm port-manager /scripts/get-service-port.sh $service)
    if [ ! -z "$PORT" ]; then
        echo "Using port $PORT for $service"
        PORT=$PORT docker-compose up -d $service
    else
        echo "Failed to get port for $service"
        exit 1
    fi
done

echo "All services started. Checking health..."
docker-compose ps 