#!/bin/bash

# Exit on any error
set -e

echo "Building services..."
./mvnw clean install -DskipTests

echo "Creating logs directory..."
mkdir -p logs

echo "Starting Eureka Server..."
java -jar service-discovery/target/service-discovery-*.jar \
    --spring.profiles.active=development \
    --server.port=8761 \
    > logs/eureka-test.log 2>&1 &

echo "Waiting for Eureka Server to start..."
sleep 10

echo "Starting Port Manager..."
java -jar services/port-manager/target/port-manager-*.jar \
    --spring.profiles.active=development \
    --server.port=8090 \
    > logs/port-manager-test.log 2>&1 &

echo "Services started. Check logs/eureka-test.log and logs/port-manager-test.log for details." 