# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml . 
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime image with OpenJDK 17 and Python 3.10
FROM openjdk:17-slim

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Install Python 3.10, ffmpeg, curl, certificates, and pip
RUN apt-get update && \
    apt-get install -y \
        python3.10 \
        python3.10-venv \
        python3.10-distutils \
        python3-pip \
        ffmpeg \
        curl \
        ca-certificates \
        gnupg \
        software-properties-common && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Upgrade pip and install yt-dlp latest version
RUN python3.10 -m ensurepip && \
    python3.10 -m pip install --upgrade pip && \
    python3.10 -m pip install --no-cache-dir yt-dlp

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties if needed
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port 8080
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
