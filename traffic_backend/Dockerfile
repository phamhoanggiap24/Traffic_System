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

# Gom toàn bộ biến môi trường vào cấu hình Java
CMD java -Dspring.datasource.url=${SPRING_DATASOURCE_URL} \
         -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME} \
         -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD} \
         -Dspring.datasource.driver-class-name=${SPRING_DATASOURCE_DRIVER_CLASS_NAME} \
         -Dspring.jpa.properties.hibernate.dialect=${SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT} \
         -Djwt.secret=${JWT_SECRET} \
         -Dtomtom.api.key=${TOMTOM_API_KEY} \
         -Dspring.mail.host=${MAIL_HOST} \
         -Dspring.mail.port=${MAIL_PORT} \
         -Dspring.mail.username=${MAIL_USERNAME} \
         -Dspring.mail.password=${MAIL_PASSWORD} \
         -jar app.jar