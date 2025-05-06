FROM openjdk:17-jdk-slim

# Install necessary tools for port management
RUN apt-get update && apt-get install -y lsof curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/port-manager-*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"] 