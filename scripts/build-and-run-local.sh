#!/bin/bash

# Set environment variables for local development
export SPRING_PROFILES_ACTIVE=dev
export MONGODB_URI="mongodb+srv://jbarseneau:Cheech99@cluster0.rd3fpku.mongodb.net/website-catalog"
export PORT_MANAGER_URL="http://localhost:8090"

# Enhanced cleanup function for local environment
cleanup_processes() {
    echo "Performing comprehensive cleanup of local environment..."
    
    # Kill all related Java processes
    echo "Stopping Java services..."
    pkill -f "java.*eureka-server" || true
    pkill -f "java.*api-gateway" || true
    pkill -f "java.*nlp-service" || true
    pkill -f "java.*catalog-processor" || true
    pkill -f "java.*url-validation" || true
    pkill -f "java.*port-manager" || true
    
    # Kill UI development server processes
    echo "Stopping UI development server..."
    pkill -f "node.*react-scripts" || true
    pkill -f "npm" || true
    
    # Check and kill processes on specific ports
    local_ports=(8761 8090 3000 8080)
    for port in "${local_ports[@]}"; do
        if lsof -ti:$port > /dev/null 2>&1; then
            echo "Killing process on port $port"
            lsof -ti:$port | xargs kill -9 2>/dev/null || true
        fi
    done
    
    # Verify ports are released
    echo "Verifying ports are released..."
    for port in "${local_ports[@]}"; do
        while lsof -ti:$port > /dev/null 2>&1; do
            echo "Waiting for port $port to be released..."
            sleep 1
        done
    done
    
    echo "Local environment cleanup complete!"
    sleep 2
}

# Function to check if a port is available
check_port() {
    local port=$1
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if ! lsof -i :"$port" >/dev/null 2>&1; then
            return 0
        fi
        echo "Port $port still in use, attempt $attempt of $max_attempts..."
        sleep 1
        attempt=$((attempt + 1))
    done
    echo "Port $port could not be freed after $max_attempts attempts"
    return 1
}

# Function to wait for service health
wait_for_service() {
    local url=$1
    local service=$2
    local max_attempts=30
    local attempt=1
    
    echo "Waiting for $service to be healthy..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url/actuator/health" 2>/dev/null | grep -q "UP"; then
            echo "$service is healthy!"
            return 0
        fi
        echo "Attempt $attempt of $max_attempts: $service not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done
    echo "WARNING: $service failed to become healthy after $max_attempts attempts"
    return 1
}

# Clean up existing processes
echo "=== Starting Local Development Environment Setup ==="
echo "Cleaning up existing processes..."
cleanup_processes

# Create logs directory for local development
mkdir -p logs

# Build all services
echo "Building all services for local development..."
./mvnw clean package -DskipTests

# Start services in correct order with health checks
echo "Starting Eureka Server..."
java -jar service-discovery/target/service-discovery-1.0-SNAPSHOT.jar > logs/eureka.log 2>&1 &
wait_for_service "http://localhost:8761" "Eureka Server"

echo "Starting Port Manager..."
java -jar common/port-manager/target/port-manager-1.0-SNAPSHOT.jar --server.port=8090 > logs/port-manager.log 2>&1 &
wait_for_service "http://localhost:8090" "Port Manager"

echo "Starting API Gateway..."
java -jar api-gateway/target/api-gateway-1.0-SNAPSHOT.jar > logs/api-gateway.log 2>&1 &
wait_for_service "http://localhost:8080" "API Gateway"

# Start microservices with dynamic ports
echo "Starting microservices..."
services=("url-validation" "nlp-service" "catalog-processor")
for service in "${services[@]}"; do
    echo "Starting $service..."
    java -jar services/$service/target/$service-1.0-SNAPSHOT.jar --server.port=0 > logs/$service.log 2>&1 &
    sleep 5  # Give each service time to register with Eureka
done

# Start UI development server last
echo "Starting UI development server..."
cd ui && npm install && npm run dev > ../logs/ui.log 2>&1 &

echo "=== Local Development Environment Setup Complete ==="
echo "Services available at:"
echo "- UI: http://localhost:3000"
echo "- Eureka Dashboard: http://localhost:8761"
echo "- Port Manager: http://localhost:8090"
echo "- API Gateway: http://localhost:8080"
echo ""
echo "Monitoring service logs..."
echo "Use Ctrl+C to stop monitoring logs (services will continue running)"
echo "To stop all services, run: ./stop-services.sh"
tail -f logs/*.log 