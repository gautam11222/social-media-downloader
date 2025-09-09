# -------------------------
# Stage 1: Build Java app
# -------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven config and source code
COPY pom.xml .
COPY src ./src

# Build the project (skip tests for faster builds)
RUN mvn clean package -DskipTests

# -------------------------
# Stage 2: Runtime
# -------------------------
FROM python:3.11-slim

WORKDIR /app

# Install Java runtime, ffmpeg, curl
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR from Stage 1
COPY --from=build /app/target/*.jar ./app.jar

# Install yt-dlp
RUN pip install --no-cache-dir yt-dlp

# Default command to run the Java app
CMD ["java", "-jar", "app.jar"]
