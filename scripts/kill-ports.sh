#!/bin/bash

echo "🔍 Checking for processes on development ports..."

PORTS=(3000 8761 8090 8081 8082 8083)
SERVICES=(
  "UI (3000)"
  "Eureka Server (8761)"
  "Port Manager (8090)"
  "URL Validation (8081)"
  "NLP Service (8082)"
  "Catalog Processor (8083)"
)

for i in "${!PORTS[@]}"; do
  PORT=${PORTS[$i]}
  SERVICE=${SERVICES[$i]}
  PID=$(lsof -ti:$PORT)
  if [ ! -z "$PID" ]; then
    echo "📍 Found ${SERVICE} running on port ${PORT} (PID: ${PID})"
    kill -9 $PID
    echo "✅ Stopped ${SERVICE}"
  fi
done

echo "✨ All development ports are clear" 