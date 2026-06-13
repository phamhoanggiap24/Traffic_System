# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /app/target/traffic_backend-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Spring Boot tự bốc hết ENV trên Render map vào file properties
CMD ["java", "-jar", "app.jar"]