#!/bin/bash
echo "Starting Space Data Archive System..."

# Load MongoDB credentials
if [ -f "./mongodb.env" ]; then
  source ./mongodb.env
  echo "MongoDB credentials loaded"
else
  echo "Error: mongodb.env not found"
  exit 1
fi

# Export for docker-compose
export MONGO_USER
export MONGO_PASSWORD
export MONGO_CLUSTER

# Build images if they don't exist
echo "Building Docker images..."
if [ -f "./build-docker-images.sh" ]; then
  chmod +x ./build-docker-images.sh
  ./build-docker-images.sh
else
  echo "Error: build-docker-images.sh not found"
  exit 1
fi

# Start services
echo "Starting services with Docker Compose..."
docker-compose up -d

echo "Services started. Available at:"
echo "- Eureka: http://localhost:8761"
echo "- API Gateway: http://localhost:8080"
echo "- URL Validation: http://localhost:8081"

# Start desktop if requested
if [ "$1" = "with-desktop" ]; then
  echo "Starting desktop client..."
  cd ../SpaceDataArchiveJava
  chmod +x run.sh
  ./run.sh
fi 