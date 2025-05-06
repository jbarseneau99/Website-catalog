# Microservices Project

This project follows a standard microservices architecture with the following structure:

## Project Structure

```
├── infrastructure/           # Core infrastructure services
│   ├── service-discovery/   # Service discovery (Eureka)
│   ├── api-gateway/        # API Gateway
│   └── config-server/      # Configuration server
│
├── services/               # Business domain services
│   ├── catalog-processor/ # Catalog processing service
│   ├── nlp-service/       # Natural Language Processing service
│   ├── port-manager/      # Port management service
│   └── url-validation/    # URL validation service
│
├── common/                # Shared libraries and utilities
│   └── src/              # Common code used across services
│
├── ui/                    # Frontend application
│
├── deployment/           # Deployment and infrastructure as code
│   ├── kubernetes/      # Kubernetes manifests
│   ├── docker/          # Docker compose files
│   ├── scripts/         # Deployment and utility scripts
│   └── environments/    # Environment-specific configurations
│
└── docs/                # Project documentation
```

## Services

### Infrastructure Services

1. **Service Discovery (Eureka)**
   - Service registration and discovery
   - Location: `infrastructure/service-discovery`

2. **API Gateway**
   - Route management and load balancing
   - Location: `infrastructure/api-gateway`

3. **Config Server**
   - Centralized configuration management
   - Location: `infrastructure/config-server`

### Business Services

1. **Catalog Processor**
   - Location: `services/catalog-processor`
   - Purpose: Process and manage catalog data

2. **NLP Service**
   - Location: `services/nlp-service`
   - Purpose: Natural language processing capabilities

3. **Port Manager**
   - Location: `services/port-manager`
   - Purpose: Manage port allocations and configurations

4. **URL Validation**
   - Location: `services/url-validation`
   - Purpose: Validate and process URLs

## Development

### Prerequisites
- Java 17+
- Maven
- Docker
- Kubernetes (optional)

### Building
```bash
# Build all services
./deployment/scripts/build.sh

# Build specific service
cd services/<service-name>
mvn clean package
```

### Running Locally
```bash
# Start all services using Docker Compose
cd deployment/docker
docker-compose up

# Start specific service
cd services/<service-name>
mvn spring-boot:run
```

### Deployment
```bash
# Deploy to Kubernetes
cd deployment/kubernetes
kubectl apply -f .
```

## Contributing

1. Each service should follow the standard Maven project structure
2. Use the common library for shared code
3. Follow the established coding standards
4. Write unit tests for new features
5. Update documentation as needed

## License

[Your License Here] 