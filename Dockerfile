# Multi-stage build để tối ưu size
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper và pom.xml trước để tận dụng Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (layer này sẽ được cache nếu pom.xml không thay đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build application (skip tests để build nhanh hơn)
RUN ./mvnw clean package -DskipTests -B

# Runtime stage - sử dụng JRE thay vì JDK để giảm size tối đa
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install wget cho health check
RUN apk add --no-cache wget

# Tạo user non-root cho security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR file từ builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Set JVM options để tối ưu memory và startup time
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=80.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.main.lazy-initialization=true"

# Chạy app với profile prod (sử dụng application-prod.yaml) và tắt logging
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod --logging.level.root=OFF"] 