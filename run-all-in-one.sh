#!/bin/bash

# Exit on error
set -e

echo "=========================================================="
echo "       SPACE DATA ARCHIVE SYSTEM - ALL-IN-ONE RUNNER       "
echo "=========================================================="

# Main execution logic
echo "Starting build and run process..."

# Check if MongoDB credentials are set
if [ -z "$MONGO_USER" ] || [ -z "$MONGO_PASSWORD" ] || [ -z "$MONGO_CLUSTER" ]; then
  echo "Setting MongoDB environment variables from config file..."
  
  # Check if config file exists
  if [ -f "./mongodb.env" ]; then
    source ./mongodb.env
    echo "MongoDB credentials loaded from mongodb.env"
  else
    echo "MongoDB credentials not found. Creating sample config file..."
    cat > mongodb.env << MONGO_EOF
# MongoDB Atlas credentials
export MONGO_USER=your_username
export MONGO_PASSWORD=your_password
export MONGO_CLUSTER=your_cluster.mongodb.net
MONGO_EOF
    echo "Please edit mongodb.env with your MongoDB credentials and run this script again."
    exit 1
  fi
fi

# Export MongoDB variables so docker-compose can use them
export MONGO_USER
export MONGO_PASSWORD
export MONGO_CLUSTER

echo "Using MongoDB cluster: $MONGO_CLUSTER"

# Make scripts executable
chmod +x build-docker-images.sh || echo "Warning: Could not make build-docker-images.sh executable"
chmod +x deploy-to-kubernetes.sh || echo "Warning: Could not make deploy-to-kubernetes.sh executable"

# Build microservices Docker images
echo "Building Docker images..."
./build-docker-images.sh

# Run with Docker Compose
echo "Starting all services with Docker Compose..."
docker-compose up -d

echo "All services started!"
echo "=========================================================="
echo "Access points:"
echo "- Eureka Server: http://localhost:8761"
echo "- API Gateway: http://localhost:8080"
echo "- URL Validation Service: http://localhost:8081/validation"
echo "=========================================================="

# Check if desktop client should be started
if [ "$1" = "with-desktop" ]; then
  echo "Starting desktop client..."
  if [ -d "../SpaceDataArchiveJava" ]; then
    cd ../SpaceDataArchiveJava
    if [ -f "run.sh" ]; then
      chmod +x run.sh
      ./run.sh
    else
      echo "Error: run.sh not found in SpaceDataArchiveJava directory"
      exit 1
    fi
  else
    echo "Error: SpaceDataArchiveJava directory not found"
    exit 1
  fi
fi 