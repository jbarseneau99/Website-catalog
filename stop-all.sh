#!/bin/bash

echo "Stopping all Space Data Archive System services..."

# Stop Docker Compose services
echo "Stopping microservices..."
docker-compose down

# Check if desktop client is running and stop it
if pgrep -f "java -jar.*SpaceDataArchiveJava" > /dev/null; then
  echo "Stopping desktop client..."
  pkill -f "java -jar.*SpaceDataArchiveJava"
fi

echo "All services stopped successfully!" 