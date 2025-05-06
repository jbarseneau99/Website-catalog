# Catalog Processor Service

## Overview
The Catalog Processor Service is responsible for processing and managing catalog data within the microservices architecture.

## Features
- Catalog data processing
- Data validation and transformation
- Integration with MongoDB for persistence
- RESTful API endpoints

## Technical Stack
- Java 17
- Spring Boot
- Spring Cloud Netflix (Eureka Client)
- MongoDB
- Maven

## API Endpoints

### Process Catalog
```
POST /api/catalog/process
Content-Type: application/json

{
    "url": "string",
    "options": {
        "validateUrls": boolean,
        "processContent": boolean
    }
}
```

### Get Processing Status
```
GET /api/catalog/status/{processId}
```

## Configuration

### Application Properties
Key configuration properties in `application.yml`:
```yaml
server:
  port: 8083

spring:
  application:
    name: CATALOG-PROCESSOR
  data:
    mongodb:
      uri: ${MONGODB_URI}

catalog-processor:
  concurrent-processes: 10
  max-processing-time-minutes: 60
  auto-validate-urls: true
  processing-batch-size: 100
```

### Environment Variables
- `MONGODB_URI`: MongoDB connection string
- `EUREKA_URL`: Eureka server URL (default: http://localhost:8761/eureka/)
- `PORT`: Service port (default: 8083)

## Building
```bash
mvn clean package
```

## Running Locally
```bash
mvn spring-boot:run
```

## Docker
```bash
# Build
docker build -t catalog-processor .

# Run
docker run -p 8083:8083 \
  -e MONGODB_URI=your_mongodb_uri \
  -e EUREKA_URL=http://eureka-server:8761/eureka/ \
  catalog-processor
```

## Health Check
The service exposes health endpoints through Spring Boot Actuator:
- Health: `/actuator/health`
- Info: `/actuator/info`
- Metrics: `/actuator/metrics`

## Dependencies
- Service Discovery (Eureka Server)
- MongoDB
- URL Validation Service (optional)
- NLP Service (optional)

## Contributing
1. Follow the standard Maven project structure
2. Use the common library for shared code
3. Write unit tests for new features
4. Update documentation as needed
5. Follow code style guidelines 