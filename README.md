# Website Catalog Microservices

This project is a microservices-based website catalog system with three deployment environments: development, staging, and production.

## Environment Setup

### Development Environment
- Uses JAR files with Atlas Hosted MongoDB
- Local development with direct JAR execution
- Services run on localhost with defined ports

```bash
# Start development environment
cd scripts
./start-microservices.sh -e dev
```

### Staging Environment
- Uses Docker Desktop with Atlas Hosted MongoDB
- Containerized services with Docker Compose
- Automated deployment through GitHub Actions

```bash
# Start staging environment
docker-compose -f docker-compose.staging.yml up -d
```

### Production Environment
- Uses GitHub-hosted runners with Atlas Hosted MongoDB
- Containerized services with high availability
- Protected deployment with manual approval
- Multiple replicas for each service

```bash
# Start production environment
docker-compose -f docker-compose.production.yml up -d
```

## Services

1. Eureka Server (Service Discovery)
   - Port: 8761
   - Process Name: EUREKA-SERVER

2. Port Manager
   - Port: 8090
   - Process Name: PORT-MANAGER

3. URL Validation Service
   - Port: 8100
   - Process Name: URL-VALIDATION

4. NLP Service
   - Port: 8101
   - Process Name: NLP-SERVICE

5. Catalog Processor
   - Port: 8102
   - Process Name: CATALOG-PROCESSOR

6. UI Application
   - Port: 3000
   - React-based frontend

## GitHub Workflow

1. Development:
   - Create feature branch from development
   - Make changes and commit
   - Create PR to development branch
   - Automated tests run
   - Merge if approved

2. Staging:
   - Create PR from development to staging
   - Automated tests and Docker builds
   - Deploy to staging environment
   - Test in staging

3. Production:
   - Create PR from staging to production
   - Requires manual approval
   - Automated deployment with high availability
   - Multiple replicas for each service

## Required Environment Variables

- `MONGODB_URI`: MongoDB Atlas connection string
- `DOCKER_USERNAME`: Docker Hub username
- `DOCKER_PASSWORD`: Docker Hub password

## Setup Instructions

1. Clone the repository
2. Set up environment variables in GitHub repository settings
3. Create environments (development, staging, production)
4. Configure branch protection rules
5. Set up required secrets for each environment

## Development Workflow

1. Create feature branch:
```bash
git checkout -b feature/your-feature development
```

2. Make changes and commit:
```bash
git add .
git commit -m "Your commit message"
```

3. Push changes:
```bash
git push origin feature/your-feature
```

4. Create Pull Request to development branch

5. After approval and merge, create PR to staging

6. After testing in staging, create PR to production 