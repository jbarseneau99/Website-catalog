#!/bin/bash

# Exit on error
set -e

echo "Deploying Space Data Archive System to Kubernetes..."

# Create MongoDB secrets first
echo "Creating MongoDB secrets..."
kubectl apply -f kubernetes/mongodb-secret.yaml

# Deploy Eureka server
echo "Deploying Eureka server..."
kubectl apply -f kubernetes/eureka/

# Wait for Eureka to be ready
echo "Waiting for Eureka server to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/eureka-server

# Deploy API Gateway
echo "Deploying API Gateway..."
kubectl apply -f kubernetes/api-gateway/

# Deploy microservices
echo "Deploying URL Validation service..."
kubectl apply -f kubernetes/url-validation/

echo "Deploying NLP service..."
kubectl apply -f kubernetes/nlp-service/

echo "Deploying Catalog Processor service..."
kubectl apply -f kubernetes/catalog-processor/

echo "All services deployed! Checking status..."
kubectl get pods

echo "To access the API Gateway from outside the cluster:"
echo "kubectl port-forward svc/api-gateway 8080:8080" 