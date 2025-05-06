FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/url-validation-*.jar app.jar

# Expose both main and management ports
EXPOSE ${PORT:-8081}
EXPOSE ${MANAGEMENT_PORT:-8081}

# Set default environment variables
ENV PORT=8081 \
    MANAGEMENT_PORT=8081 \
    EUREKA_URL=http://localhost:8761 \
    PORT_MANAGER_URL=http://localhost:8090

HEALTHCHECK --interval=30s --timeout=3s \
    CMD curl -f http://localhost:${MANAGEMENT_PORT:-8081}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-DMACH33_NAME=Mach33_URL_Validation", "app.jar"] 