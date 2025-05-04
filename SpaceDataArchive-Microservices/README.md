# Space Data Archive Microservices

This project implements a microservices architecture for the Space Data Archive system, enabling distributed processing of catalog data with improved scalability and resilience.

## Architecture

The system consists of the following components:

1. **Service Discovery (Eureka)** - Allows services to find and communicate with each other
2. **API Gateway** - Entry point for clients that routes requests to appropriate services
3. **URL Validation Service** - Validates URLs and provides health information about web resources
4. **Catalog Processor Service** - Processes and manages space data catalogs
5. **NLP Service** - Provides natural language processing capabilities

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB Atlas account (configured and credentials available)

## Database Configuration

The services use MongoDB Atlas for data storage. Each service connects to a separate database within the same MongoDB Atlas cluster:

- URL Validation Service: `url_validation` database
- Catalog Processor Service: `catalog_processor` database
- NLP Service: `nlp_service` database

## Building the Project

```bash
mvn clean install
```

## Running the Services

Start the services in the following order:

1. **Service Discovery**:
   ```bash
   cd service-discovery
   mvn spring-boot:run
   ```

2. **API Gateway**:
   ```bash
   cd api-gateway
   mvn spring-boot:run
   ```

3. **URL Validation Service**:
   ```bash
   cd services/url-validation
   mvn spring-boot:run
   ```

4. **Catalog Processor Service**:
   ```bash
   cd services/catalog-processor
   mvn spring-boot:run
   ```

5. **NLP Service**:
   ```bash
   cd services/nlp-service
   mvn spring-boot:run
   ```

Alternatively, you can use the provided shell script to start all services at once:

```bash
./run-all.sh
```

## Service Endpoints

Once the services are running, you can access them via the API Gateway:

- Service Discovery Dashboard: http://localhost:8761
- URL Validation API: http://localhost:8080/api/validation/
- Catalog Processor API: http://localhost:8080/api/catalog/
- NLP Service API: http://localhost:8080/api/nlp/

## Integration with Existing System

The microservices can be integrated with the existing JavaFX desktop application by configuring the desktop app to communicate with the API Gateway instead of using the embedded services.

### Benefits of This Approach

1. Scalability - Services can be deployed and scaled independently
2. Resilience - Failure in one service doesn't bring down the entire system
3. Technology Diversity - Different services can use different technologies as needed
4. Multiple Clients - Support for both desktop and web clients
5. Distributed Processing - Enable parallel processing of large datasets

## Development

To add new functionality:

1. Define interfaces and DTOs in the `common` module
2. Implement the service in the appropriate microservice
3. Update the API Gateway configuration for routing

## Security

- Services authenticate with Eureka using basic authentication
- The API Gateway handles client authentication and authorization
- Inter-service communication is secured through appropriate mechanisms

## Monitoring

Each service exposes health and metrics endpoints via Spring Boot Actuator, which can be monitored using tools like Prometheus and Grafana.

## Configuration

Services are configured via their respective `application.yml` files. Configuration properties can be externalized using Spring Cloud Config for centralized management. 