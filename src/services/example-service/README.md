# Example Microservice

This is a template for new microservices following the improved project structure.

## Structure

```
example-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── service/
│       └── resources/
│           ├── application.yml
│           └── bootstrap.yml
├── Dockerfile
└── README.md
```

## Getting Started

To create a new service based on this template:

1. Copy this directory to a new location in `src/services/`
2. Rename the directory and update the package structure
3. Update the service name in `application.yml`
4. Implement your service logic

## Integration with Platform

The service should:

1. Register with Eureka service discovery
2. Expose health endpoints
3. Configure MongoDB connection (if needed)
4. Document APIs with Swagger

## Build and Run

```bash
# Build the service
./bin/run-platform.sh build --service your-service-name

# Run in local environment
./bin/run-platform.sh --local
```

## Next Steps

Add specific functionality to your service and document its APIs. 