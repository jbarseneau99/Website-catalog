#!/bin/bash
echo "Starting Space Data Archive System..."
source ../mongodb.env
export MONGO_USER MONGO_PASSWORD MONGO_CLUSTER
if ! docker info >/dev/null 2>&1; then echo "Error: Docker is not running"; exit 1; fi
echo "Using MongoDB cluster: $MONGO_CLUSTER"
docker-compose up -d
