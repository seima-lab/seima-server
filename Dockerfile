# Multi-stage build for Spring Boot application
# Stage 1: Build stage
FROM maven:3.9.6-amazoncorretto-21-alpine AS build

# Install git for potential dependencies
RUN apk add --no-cache git

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better Docker layer caching
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B --no-transfer-progress

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B --no-transfer-progress

# Stage 2: Runtime stage
FROM amazoncorretto:21-alpine AS runtime

# Labels for image management
LABEL maintainer="SEIMA Team"
LABEL version="1.0.0"
LABEL description="SEIMA Server Spring Boot Application"

# Install essential packages
RUN apk add --no-cache curl dumb-init tzdata && \
    rm -rf /var/cache/apk/*

# Create a non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the jar file from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Set timezone
ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Simple health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Essential environment variables for production
ENV SPRING_PROFILES_ACTIVE=prod

# Simple but effective JVM settings for production
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport -Djava.security.egd=file:/dev/./urandom"

# Run with proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 