# Build stage: use Maven with JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the Java application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Runtime stage: use slim Python 3.10 with Java
FROM python:3.10-slim

WORKDIR /app

# Install dependencies: Java runtime, ffmpeg, curl, python venv
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    ffmpeg \
    curl \
    python3-venv \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts if any
COPY ./*.py ./ 

# Copy requirements.txt and install (safe if empty)
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt || true

# Expose application port (change if needed)
EXPOSE 8080

# Default command: run Java app
CMD ["java", "-jar", "app.jar"]
