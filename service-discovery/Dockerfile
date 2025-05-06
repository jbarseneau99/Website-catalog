FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/service-discovery-1.0-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"] 