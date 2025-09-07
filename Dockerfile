# Stage 1: Build
FROM maven:3.9.3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy entire project (includes .mvn and mvnw)
COPY . .

# Fix permissions for mvnw
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    python3 \
    python3-pip \
    bash \
    clamav \
    clamav-daemon \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp (FIX: Add --break-system-packages flag)
RUN pip3 install --no-cache-dir yt-dlp --break-system-packages

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy entrypoint script
COPY entrypoint.sh ./
RUN chmod +x entrypoint.sh

# Create temp directory
RUN mkdir -p /tmp/socialdownloader

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
