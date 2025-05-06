FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/nlp-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8101

ENTRYPOINT ["java", "-jar", "app.jar"] 