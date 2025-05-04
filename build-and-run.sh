#!/bin/bash

# Exit on error
set -e

echo "Building and running Space Data Archive System..."

# Source MongoDB credentials
source mongodb.env

# Build all services
echo "Building Maven projects..."
mvn clean package -DskipTests

# Build Docker images
echo "Building Docker images..."
docker build -t spacedataarchive/eureka-server:latest service-discovery/
docker build -t spacedataarchive/api-gateway:latest api-gateway/
docker build -t spacedataarchive/nlp-service:latest services/nlp-service/
docker build -t spacedataarchive/catalog-processor:latest services/catalog-processor/
docker build -t spacedataarchive/url-validation:latest services/url-validation/

# Create Kubernetes secrets for MongoDB
echo "Creating Kubernetes secrets..."
kubectl create secret generic mongodb-credentials \
  --from-literal=nlp-service-connection-string="mongodb+srv://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CLUSTER}/nlp_service" \
  --from-literal=catalog-processor-connection-string="mongodb+srv://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CLUSTER}/catalog_processor" \
  --from-literal=url-validation-connection-string="mongodb+srv://${MONGO_USER}:${MONGO_PASSWORD}@${MONGO_CLUSTER}/url_validation" \
  --dry-run=client -o yaml | kubectl apply -f -

# Apply Kubernetes configurations
echo "Applying Kubernetes configurations..."
kubectl apply -f kubernetes/eureka/deployment.yaml
kubectl apply -f kubernetes/api-gateway/deployment.yaml
kubectl apply -f kubernetes/catalog-processor/deployment.yaml
kubectl apply -f kubernetes/nlp-service/deployment.yaml
kubectl apply -f kubernetes/url-validation/deployment.yaml

echo "All services have been built and deployed!"
echo "You can monitor the services using:"
echo "kubectl get pods -w" 