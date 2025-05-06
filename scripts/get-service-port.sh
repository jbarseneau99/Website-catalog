#!/bin/bash

SERVICE_NAME=$1
PORT_MANAGER_URL=${PORT_MANAGER_URL:-http://localhost:8090}
MAX_RETRIES=30
RETRY_INTERVAL=2

# Function to get port from Port Manager
get_port() {
    local service=$1
    curl -s "${PORT_MANAGER_URL}/ports/${service}" | grep -o '"port":[0-9]*' | cut -d':' -f2
}

# Wait for Port Manager to be available
echo "Waiting for Port Manager to be available..."
until curl -s "${PORT_MANAGER_URL}/actuator/health" | grep -q '"status":"UP"'; do
    sleep 2
done

# Get port with retries
for ((i=1; i<=$MAX_RETRIES; i++)); do
    PORT=$(get_port "$SERVICE_NAME")
    if [ ! -z "$PORT" ]; then
        echo "$PORT"
        exit 0
    fi
    echo "Attempt $i: Waiting for port assignment for $SERVICE_NAME..."
    sleep $RETRY_INTERVAL
done

# If we get here, we failed to get a port
echo "Failed to get port for $SERVICE_NAME after $MAX_RETRIES attempts" >&2
exit 1 