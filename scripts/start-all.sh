#!/bin/bash

set -e # Exit on any error

# Function to check if a port is available
check_port() {
    local port=$1
    local service=$2
    local max_attempts=30
    local attempt=1

    echo "⏳ Waiting for $service to start on port $port..."
    while ! nc -z localhost $port; do
        if [ $attempt -eq $max_attempts ]; then
            echo "❌ $service failed to start on port $port after $max_attempts attempts"
            exit 1
        fi
        attempt=$((attempt+1))
        sleep 1
    done
    echo "✅ $service is running on port $port"
}

# Function to wait for service health
wait_for_health() {
    local url=$1
    local service=$2
    local max_attempts=30
    local attempt=1

    echo "🏥 Checking $service health..."
    while ! curl -s $url > /dev/null; do
        if [ $attempt -eq $max_attempts ]; then
            echo "❌ $service health check failed after $max_attempts attempts"
            exit 1
        fi
        attempt=$((attempt+1))
        sleep 1
    done
    echo "✅ $service is healthy"
}

echo "🚀 Starting Website Catalog development environment..."

# 1. Ensure clean environment
echo "🧹 Cleaning up existing processes..."
bash scripts/kill-ports.sh

# 2. Start MongoDB
echo "📦 Starting MongoDB..."
if ! pgrep mongod > /dev/null; then
    brew services start mongodb-community
    sleep 2
fi

# 3. Start Eureka Server
echo "🔍 Starting Eureka Server..."
java -jar services/eureka-server/target/eureka-server.jar &
check_port 8761 "Eureka Server"
wait_for_health "http://localhost:8761/actuator/health" "Eureka Server"

# 4. Start Port Manager
echo "🎯 Starting Port Manager..."
java -jar services/port-manager/target/port-manager.jar &
check_port 8090 "Port Manager"
wait_for_health "http://localhost:8090/actuator/health" "Port Manager"

# 5. Start all other services found in the services directory
echo "🚀 Starting microservices..."
for service_dir in services/*/; do
    if [ "$service_dir" != "services/eureka-server/" ] && [ "$service_dir" != "services/port-manager/" ]; then
        if [ -f "${service_dir}target/"*.jar ]; then
            service_name=$(basename "$service_dir")
            echo "Starting $service_name..."
            java -jar "${service_dir}target/"*.jar &
            sleep 2  # Give each service time to start
        fi
    fi
done

# 6. Start UI
echo "🖥️ Starting UI..."
cd ui && npm start &

# 7. Final health check
echo "🏥 Performing final health check..."
bash scripts/check-status.sh

echo "✨ Development environment is ready!"
echo "📝 Access points:"
echo "  - UI: http://localhost:3000"
echo "  - Eureka: http://localhost:8761"
echo "  - Port Manager: http://localhost:8090" 