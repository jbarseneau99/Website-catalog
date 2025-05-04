#!/bin/bash
echo "Stopping all services..."
docker-compose down
pkill -f "java -jar.*SpaceDataArchiveJava" || true
echo "All services stopped!"
