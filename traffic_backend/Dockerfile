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

# Gom toàn bộ biến môi trường vào cấu hình Java trước khi chạy
ENV JAVA_OPTS="-Dspring.datasource.url=${SPRING_DATASOURCE_URL} \
               -Dspring.datasource.username=${SPRING_DATASOURCE_USERNAME} \
               -Dspring.datasource.password=${SPRING_DATASOURCE_PASSWORD} \
               -Djwt.secret=${JWT_SECRET} \
               -Dtomtom.api.key=${TOMTOM_API_KEY}"

# Lệnh khởi chạy ứng dụng
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]