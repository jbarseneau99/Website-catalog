FROM openjdk:17-jdk-slim

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/url-validation-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-DMACH33_NAME=Mach33_URL_Validation", "app.jar"] 