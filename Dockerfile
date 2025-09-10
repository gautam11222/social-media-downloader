# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

ENV APP_HOME=/app

WORKDIR ${APP_HOME}

# Install dependencies, python3, pip, ffmpeg, and yt-dlp pinned to version 2023.10.10
RUN apt-get update && \
    apt-get install -y ffmpeg curl ca-certificates python3 python3-pip && \
    pip3 install yt-dlp==2023.10.10 && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties if needed
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port 8080
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
