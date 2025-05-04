# Space Data Archive System - Run Instructions

This document provides instructions for running the Space Data Archive System using the containerized microservices architecture.

## Prerequisites

Before running the system, ensure you have the following installed:

- Docker and Docker Compose
- Java 17 or higher (for the desktop client)
- MongoDB Atlas account with connection credentials

## Quick Start

### Option 1: All-in-One Script

For the simplest setup, use our all-in-one script:

1. Set up MongoDB credentials by creating a `mongodb.env` file:
   ```bash
   cp mongodb.env.template mongodb.env
   # Edit mongodb.env with your actual credentials
   ```

2. Run everything with a single command:
   ```bash
   ./run-all-in-one.sh
   ```

3. To also start the desktop client:
   ```bash
   ./run-all-in-one.sh with-desktop
   ```

4. To stop all services:
   ```bash
   ./stop-all.sh
   ```

### Option 2: Manual Steps

If you prefer to run services individually:

1. Set MongoDB environment variables:
   ```bash
   export MONGO_USER=your_mongodb_username
   export MONGO_PASSWORD=your_mongodb_password
   export MONGO_CLUSTER=your_mongodb_cluster.mongodb.net
   ```

2. Build Docker images:
   ```bash
   ./build-docker-images.sh
   ```

3. Start microservices:
   ```bash
   docker-compose up -d
   ```

4. Run the desktop client (optional):
   ```bash
   cd ../SpaceDataArchiveJava
   ./run.sh
   ```

## Accessing the System

Once running, you can access the system through:

- **Service Discovery (Eureka)**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **URL Validation API**: http://localhost:8081/validation/url
- **NLP Service API**: http://localhost:8082
- **Catalog Processor API**: http://localhost:8083

## UI Options

1. **Web UI**: Access services through API Gateway at http://localhost:8080
2. **Desktop Client**: JavaFX application started with the `with-desktop` option

## Monitoring and Troubleshooting

- View all running containers: `docker ps`
- Check service logs: `docker-compose logs -f [service-name]`
- Check health endpoints: `curl http://localhost:8080/actuator/health`

## Kubernetes Deployment (Production)

For production deployment to Kubernetes:

```bash
./deploy-to-kubernetes.sh
```

See `CONTAINERIZATION-BENEFITS.md` for more information about the containerized architecture benefits. 