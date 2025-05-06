FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=staging

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"] 