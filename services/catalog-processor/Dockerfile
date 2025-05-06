# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copy the common library and install it first
COPY ../../common/pom.xml /common/pom.xml
COPY ../../common/src /common/src
RUN cd /common && mvn install -DskipTests

# Copy the service files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-slim
WORKDIR /app

# Add health check utilities
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Copy the built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Add wait-for-it script for service dependencies
ADD https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Set environment variables
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE="docker"

# Expose the service port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Start the service
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 