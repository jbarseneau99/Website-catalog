#!/bin/bash

# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export MONGODB_URI="mongodb+srv://jbarseneau:Cheech99@cluster0.rd3fpku.mongodb.net/website-catalog-prod"
export PORT_MANAGER_URL="http://port-manager:8090"

# Function to check remote deployment status
check_deployment() {
    local deployment=$1
    local namespace=$2
    local max_attempts=30
    local attempt=1
    
    echo "Checking deployment status for $deployment..."
    while [ $attempt -le $max_attempts ]; do
        status=$(kubectl get deployment "$deployment" -n "$namespace" -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' 2>/dev/null)
        if [ "$status" == "True" ]; then
            echo "Deployment $deployment is ready"
            return 0
        fi
        echo "Attempt $attempt: Deployment $deployment not ready yet..."
        sleep 5
        attempt=$((attempt + 1))
    done
    echo "Deployment $deployment failed to become ready"
    return 1
}

# Ensure we're on main branch and up to date
echo "Updating main branch..."
git checkout main
git pull origin main

# Build all services
echo "Building all services..."
./mvnw clean package -DskipTests

# Build Docker images and push to GitHub Container Registry
echo "Building and pushing Docker images..."
docker build -t ghcr.io/jbarseneau/eureka-server:latest ./service-discovery
docker build -t ghcr.io/jbarseneau/port-manager:latest ./port-manager
docker build -t ghcr.io/jbarseneau/url-validation:latest ./url-validation
docker build -t ghcr.io/jbarseneau/nlp-service:latest ./nlp-service
docker build -t ghcr.io/jbarseneau/catalog-processor:latest ./catalog-processor
docker build -t ghcr.io/jbarseneau/ui:latest ./ui

docker push ghcr.io/jbarseneau/eureka-server:latest
docker push ghcr.io/jbarseneau/port-manager:latest
docker push ghcr.io/jbarseneau/url-validation:latest
docker push ghcr.io/jbarseneau/nlp-service:latest
docker push ghcr.io/jbarseneau/catalog-processor:latest
docker push ghcr.io/jbarseneau/ui:latest

# Apply Kubernetes configurations
echo "Applying Kubernetes configurations..."
kubectl apply -f kubernetes/namespace.yml
kubectl apply -f kubernetes/config-maps.yml
kubectl apply -f kubernetes/secrets.yml
kubectl apply -f kubernetes/services/
kubectl apply -f kubernetes/deployments/

# Wait for core services
echo "Waiting for core services to be ready..."
check_deployment "eureka-server" "website-catalog"
check_deployment "port-manager" "website-catalog"

# Wait for other services
echo "Waiting for other services to be ready..."
check_deployment "url-validation" "website-catalog"
check_deployment "nlp-service" "website-catalog"
check_deployment "catalog-processor" "website-catalog"
check_deployment "ui" "website-catalog"

# Show deployment status
echo "Deployment Status:"
kubectl get pods -n website-catalog
kubectl get services -n website-catalog

# Show service logs
echo "Service Logs:"
kubectl logs -n website-catalog -l app=website-catalog --all-containers=true -f 