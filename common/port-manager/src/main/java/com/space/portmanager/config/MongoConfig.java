server:
  port: 8085

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
  instance:
    preferIpAddress: true
    instanceId: ${spring.application.name}:8085
    nonSecurePort: 8085
    nonSecurePortEnabled: true

spring:
  application:
    name: MACH33-llm-connection
  data:
    mongodb:
      uri: ${MONGODB_URI}
      database: llm-connection

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  level:
    root: INFO
    com.mach33: DEBUG

cloud:
  gateway:
    routes:
      - id: llm-connection
        uri: lb://MACH33-llm-connection
        predicates:
          - Path=/api/llm/**
        filters:
          - StripPrefix=2 