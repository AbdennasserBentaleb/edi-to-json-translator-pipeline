FROM maven:3.9-eclipse-temurin-23-alpine AS builder

# Note: We are using temurin 23 here for the build environment since openjdk-25-ea alpine images 
# are not officially provided as standard Maven images yet, but we will compile targeting Java 25 
# by downloading the EA JDK manually or using the system defaults if 25 is available.
# Wait, let's use a simpler approach: we'll use ubuntu and install openjdk-25-ea
# Actually, the user asked for Alpine/Distroless for Java 25.
# Let's use eclipse-temurin:25-ea-alpine if possible, or just build the app natively using the local maven 
# and ONLY wrap it in Alpine for the runtime to strictly respect the portfolio requirement.

# We will assume the application is built locally (or in CI) and we just package it in the Dockerfile.
# This makes the Distroless/Alpine image as lightweight as possible.

FROM eclipse-temurin:25-ea-alpine-jre

WORKDIR /app

# Create necessary directories for the pipeline
RUN mkdir -p /app/input /app/error

# Copy dependencies and the compiled application classes
COPY target/lib /app/lib
COPY target/classes /app/classes

# Expose the REST interface port
EXPOSE 8080

# Environment variables for configuration
ENV SERVER_PORT=8080
ENV LOGGING_LEVEL_ROOT=INFO
ENV EDI_INPUT_DIR=/app/input
ENV EDI_ERROR_DIR=/app/error

# Run the application using the standard classpath approach
ENTRYPOINT ["java", "--enable-preview", "-cp", "/app/classes:/app/lib/*", "com.portfolio.edi.EdiTranslatorApplication"]
