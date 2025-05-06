#!/bin/bash

echo "Cleaning up port assignments..."

# Try to reset all port assignments
curl -X DELETE "http://localhost:8090/api/ports/reset" \
    --fail --silent --show-error || echo "Failed to reset port assignments"

# Kill any processes that might be holding onto ports
services=(
    "java -jar.*eureka-server"
    "java -jar.*api-gateway"
    "java -jar.*nlp-service"
    "java -jar.*catalog-processor"
    "java -jar.*url-validation"
    "java -jar.*port-manager"
)

for pattern in "${services[@]}"; do
    pids=$(pgrep -f "$pattern" || true)
    if [ ! -z "$pids" ]; then
        echo "Killing processes matching: $pattern"
        pkill -f "$pattern" || true
    fi
done

# Wait for ports to be released
echo "Waiting for ports to be released..."
sleep 5

echo "Port cleanup complete." 