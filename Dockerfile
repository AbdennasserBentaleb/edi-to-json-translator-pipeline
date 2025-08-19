FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create necessary directories for the pipeline
RUN mkdir -p /app/input /app/error

# Add a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup -u 65532 \
    && chown -R appuser:appgroup /app

# Switch to the non-root user
USER 65532

# Copy dependencies and the compiled application classes
COPY --from=builder --chown=appuser:appgroup /build/target/lib /app/lib
COPY --from=builder --chown=appuser:appgroup /build/target/classes /app/classes

# Expose the REST interface port
EXPOSE 8080

# Environment variables for configuration
ENV SERVER_PORT=8080
ENV LOGGING_LEVEL_ROOT=INFO
ENV EDI_INPUT_DIR=/app/input
ENV EDI_UPLOAD_DIR=/app/input
ENV EDI_ERROR_DIR=/app/error
# By default, point the destination to itself for demo purposes
ENV EDI_REST_ENDPOINT=http://localhost:8080/api/v1/canonical

# Run the application using the standard classpath approach
ENTRYPOINT ["java", "-cp", "/app/classes:/app/lib/*", "com.portfolio.edi.EdiTranslatorApplication"]
