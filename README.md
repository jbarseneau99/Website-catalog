# Website Catalog

Repository for Cataloging Websites
Mach33-Microservices-Platform

This repository contains the Website Catalog application built on the Mach33 Microservices Platform.

## Environment Setup

This project supports three deployment environments, all using MongoDB Atlas for data persistence:

### 1. Local Environment (JAR Files)

The local environment runs the microservices directly as JAR files on your machine.

- **Technology**: Java JAR files
- **Database**: MongoDB Atlas (shared connection)
- **Purpose**: Development and testing on local machine
- **Run Command**: `./run-local.sh`
- **Requirements**:
  - Java 17+
  - Maven
  
To set up:
1. Copy `mach33-env.sh.template` to `mach33-env.sh`
2. Edit `mach33-env.sh` to configure your MongoDB Atlas connection
3. Run `./run-local.sh`

### 2. Staging Environment (Docker Desktop)

The staging environment uses Docker containers to run the services on your local machine.

- **Technology**: Docker containers on local Docker Desktop
- **Database**: MongoDB Atlas (shared connection)
- **Purpose**: Integration testing and pre-production verification
- **Run Command**: `./run-staging.sh`
- **Requirements**:
  - Docker Desktop or equivalent
  - Docker Compose

To set up:
1. Copy `mach33-env.sh.template` to `mach33-env.sh`
2. Edit `mach33-env.sh` to configure your MongoDB Atlas connection
3. Run `./run-staging.sh`

### 3. Production Environment (GitHub)

The production environment uses GitHub Actions to build, deploy, and run the services.

- **Technology**: GitHub Actions workflows
- **Database**: MongoDB Atlas (shared connection)
- **Purpose**: Production deployment
- **Run Command**: `./run-production.sh --trigger`
- **Requirements**:
  - GitHub repository with Actions enabled
  - GitHub Personal Access Token with workflow and repo permissions

To set up:
1. Configure GitHub secrets (see below)
2. Ensure `.github/workflows/deploy-production.yml` is pushed to the repository
3. Run `./run-production.sh --trigger`

### Setting Up GitHub Secrets for Production Deployment

For production deployments to work correctly, you need to set up the following secrets in your GitHub repository:

1. Go to your GitHub repository
2. Click on "Settings" > "Secrets and variables" > "Actions"
3. Add the following secrets:
   - `MONGODB_URI`: Your MongoDB Atlas connection string (e.g., mongodb+srv://username:password@host/database)

Without these secrets, the production deployment workflow will fail.

### Additional Information

- All environments use MongoDB Atlas for data persistence
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080

## Quick Start

Use the convenience script `run-platform.sh` to manage the platform:

```bash
# Start local environment
./run-platform.sh run-local

# Start staging environment
./run-platform.sh run-staging

# Start production environment
./run-platform.sh run-production

# Stop all services
./run-platform.sh stop-all
```

## Available Services

The platform includes the following services:

- **Infrastructure Services**
  - Service Discovery (Eureka): Port 8761
  - API Gateway: Port 8080

- **Microservices**
  - URL Validation: Port 8082
  - NLP Service: Port 8083
  - Catalog Processor: Port 8084
  - LLM Connection: Port 8085
  - Google Search: Port 8762

## Creating New Services

To create a new service:

```bash
./run-platform.sh create-service [service-name] [port]
```

## Accessing Service Documentation

Each service provides Swagger UI documentation available at:

```
http://localhost:[PORT]/swagger-ui/index.html
```

Service health status is available at:

```
http://localhost:[PORT]/actuator/health
```

## Monitoring

- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080

Without these secrets, the production deployment workflow will fail. 