# Space Data Archive System - Containerization and Kubernetes Deployment

This guide provides steps to containerize the Space Data Archive System microservices and deploy them to Kubernetes.

## Prerequisites

- Docker installed locally
- Maven installed for building Java applications
- A Kubernetes cluster (local like Minikube/Kind or managed service like EKS/GKE/AKS)
- kubectl configured to connect to your cluster

## Directory Structure

```
SpaceDataArchive-Microservices/
├── api-gateway/
├── eureka-server/
├── services/
│   ├── url-validation/
│   ├── nlp-service/
│   └── catalog-processor/
├── kubernetes/
│   ├── api-gateway/
│   ├── eureka/
│   ├── url-validation/
│   ├── nlp-service/
│   ├── catalog-processor/
│   └── mongodb-secret.yaml
├── docker-compose.yml
├── build-docker-images.sh
└── deploy-to-kubernetes.sh
```

## Containerization Process

1. Build the microservices and their Docker images:

```bash
# Make the script executable
chmod +x build-docker-images.sh

# Run the build script
./build-docker-images.sh
```

2. For local testing with Docker Compose:

```bash
# Set MongoDB environment variables
export MONGO_USER=your-mongodb-username
export MONGO_PASSWORD=your-mongodb-password
export MONGO_CLUSTER=your-mongodb-cluster.mongodb.net

# Start all services
docker-compose up -d
```

## Kubernetes Deployment

1. Update the MongoDB secrets with actual credentials:

```bash
# Base64 encode your MongoDB connection string
echo -n "mongodb+srv://username:password@cluster.mongodb.net/url_validation" | base64

# Edit the kubernetes/mongodb-secret.yaml file with the encoded values
```

2. Deploy to Kubernetes:

```bash
# Make the script executable
chmod +x deploy-to-kubernetes.sh

# Deploy all services
./deploy-to-kubernetes.sh
```

3. Access the API Gateway:

```bash
kubectl port-forward svc/api-gateway 8080:8080
```

The API will be accessible at http://localhost:8080

## Service Ports

- Eureka Server: 8761
- API Gateway: 8080
- URL Validation: 8081
- NLP Service: 8082
- Catalog Processor: 8083

## Scaling Services

To scale a service, use:

```bash
kubectl scale deployment url-validation --replicas=3
```

## Monitoring

To monitor the status of your deployments:

```bash
kubectl get pods
kubectl get deployments
kubectl get services
```

For detailed logs:

```bash
kubectl logs deployment/url-validation
```

## Cleanup

To remove all deployed resources:

```bash
kubectl delete -f kubernetes/
```

To stop and remove local Docker containers:

```bash
docker-compose down
``` 