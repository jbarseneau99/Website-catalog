FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/service-discovery-*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
