# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image with Python 3.10 and OpenJDK 17
FROM python:3.10-slim

ENV APP_HOME=/app

WORKDIR ${APP_HOME}

# Install Java 17, ffmpeg, curl, and certificates
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk ffmpeg curl ca-certificates && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Install yt-dlp latest version using pip
RUN pip install --no-cache-dir yt-dlp

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties if needed
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port 8080
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
