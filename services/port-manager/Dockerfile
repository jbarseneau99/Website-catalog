FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/port-manager-1.0-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=staging

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"] 