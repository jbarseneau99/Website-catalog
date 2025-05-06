#!/bin/bash

# Function to kill process on a specific port
kill_port() {
    local port=$1
    local name=$2
    echo "Checking port $port ($name)..."
    local pid=$(lsof -ti:$port)
    if [ ! -z "$pid" ]; then
        echo "Found process using port $port (PID: $pid)"
        kill -9 $pid 2>/dev/null || true
    fi
}

# Function to kill process by pattern
kill_pattern() {
    local pattern=$1
    local name=$2
    echo "Checking for $name processes..."
    pkill -f "$pattern" || true
}

echo "🧹 Cleaning up microservices ports..."

# Kill processes on all used ports
kill_port 8080 "API Gateway"
kill_port 8081 "URL Validation"
kill_port 8082 "NLP Service"
kill_port 8083 "Catalog Processor"
kill_port 8761 "Eureka Server"
kill_port 27017 "MongoDB"
kill_port 3000 "UI Development"
kill_port 3001 "UI Development (alternate)"

echo "🐳 Stopping Docker containers..."
docker-compose down --volumes --remove-orphans

echo "☕ Stopping Java processes..."
kill_pattern "java -jar.*eureka-server" "Eureka Server"
kill_pattern "java -jar.*api-gateway" "API Gateway"
kill_pattern "java -jar.*nlp-service" "NLP Service"
kill_pattern "java -jar.*catalog-processor" "Catalog Processor"
kill_pattern "java -jar.*url-validation" "URL Validation"

echo "📦 Stopping Node.js processes..."
# Kill any React development servers
kill_pattern "react-scripts.*start" "React Scripts"
kill_pattern "webpack.*development" "Webpack Dev Server"
kill_pattern "node.*npm" "NPM"

# Specific to your environment - kill the React development server
echo "🎯 Stopping React development server..."
kill_pattern "/Users/jbarseneau/Desktop/Website-catalog/ui/node_modules/react-scripts/scripts/start.js" "React Start Script"
kill_pattern "/Users/jbarseneau/nodejs/bin/node.*react-scripts" "React Scripts Node"

# Double check port 3000 specifically
echo "🔍 Double checking port 3000..."
for pid in $(ps aux | grep -i "node" | grep -i "start.js" | grep -v grep | awk '{print $2}'); do
    echo "Killing Node.js process with PID: $pid"
    kill -9 $pid 2>/dev/null || true
done

echo "✨ Cleanup complete!" 