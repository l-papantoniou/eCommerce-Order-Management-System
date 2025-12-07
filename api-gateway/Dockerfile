# Multi-stage build for API Gateway
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent POM first
COPY pom.xml .

# Copy ALL modules (parent POM references all of them)
COPY common-lib ./common-lib
COPY authorization-server ./authorization-server
COPY api-gateway ./api-gateway
COPY order-service ./order-service
COPY analytics-service ./analytics-service
COPY notification-service ./notification-service

# Build only api-gateway and its dependencies
RUN mvn clean package spring-boot:repackage -pl api-gateway -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]