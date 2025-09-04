# ========================
# Stage 1: Build the app
# ========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build Spring Boot jar
RUN mvn clean package -DskipTests

# ========================
# Stage 2: Runtime
# ========================
FROM ubuntu:24.04

# Install Java, ffmpeg, yt-dlp, clamav
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    ca-certificates \
    clamav \
    clamav-daemon \
    yt-dlp \
    && rm -rf /var/lib/apt/lists/*

# Working directory
WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Update ClamAV definitions (ignore errors if offline)
RUN freshclam || true

# Expose port
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
