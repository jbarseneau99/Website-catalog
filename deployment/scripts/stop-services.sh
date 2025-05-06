#!/bin/bash

echo "Stopping all services..."

# Stop UI development server
echo "Stopping UI server..."
pkill -f "node.*react-scripts" || true
pkill -f "npm" || true

# Stop Java services in reverse order
echo "Stopping Java services..."
pkill -f "java.*catalog-processor" || true
pkill -f "java.*nlp-service" || true
pkill -f "java.*url-validation" || true
pkill -f "java.*api-gateway" || true
pkill -f "java.*port-manager" || true
pkill -f "java.*eureka-server" || true

# Stop MongoDB
echo "Stopping MongoDB..."
brew services stop mongodb-community || true

# Verify all ports are released
echo "Verifying ports are released..."
ports=(3000 8080 8090 8761)
for port in "${ports[@]}"; do
    while lsof -ti:$port > /dev/null 2>&1; do
        echo "Waiting for port $port to be released..."
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
        sleep 1
    done
done

echo "All services stopped successfully!" 