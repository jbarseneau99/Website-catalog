#!/bin/bash

echo "Stopping all Docker containers..."
docker-compose down --volumes --remove-orphans

echo "Cleaning up any Kubernetes resources..."
kubectl delete deployment --all 2>/dev/null || true
kubectl delete service --all 2>/dev/null || true
kubectl delete pod --all 2>/dev/null || true

echo "Stopping any running Java processes (excluding IDE processes)..."
ps aux | grep java | grep -v "redhat.java" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

echo "Stopping Node.js and UI processes..."
pkill -f "node.*react-scripts" || true
pkill -f "webpack" || true
pkill -f "node" || true
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
lsof -ti:3001 | xargs kill -9 2>/dev/null || true

echo "Pruning Docker system..."
docker system prune -f

# Clean up any lingering npm processes
echo "Cleaning up npm processes..."
ps aux | grep "npm start" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

echo "Cleanup complete!" 