FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/service-discovery-*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "-DMACH33_NAME=Mach33_Eureka_Server", "app.jar"] 