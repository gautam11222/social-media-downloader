# Multi-stage build for efficiency
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Production stage
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk update && apk add --no-cache \
    python3 \
    py3-pip \
    ffmpeg \
    wget \
    curl \
    bash \
    clamav \
    freshclam \
    && rm -rf /var/cache/apk/*

# Install yt-dlp
RUN pip3 install --no-cache-dir yt-dlp

# Create app directory
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/socialdownloader.jar socialdownloader.jar

# Copy entrypoint script
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Create temp directory for downloads
RUN mkdir -p /tmp/socialdownloader

# Update ClamAV database
RUN freshclam --quiet || true

# Expose port
EXPOSE 8080

# Set JVM options for container
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
