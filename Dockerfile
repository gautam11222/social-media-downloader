# Stage 1: Build the application
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM ubuntu:22.04

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Install Python 3.10, pip, ffmpeg, curl, and other tools
RUN apt-get update && \
    apt-get install -y software-properties-common gnupg curl ca-certificates && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y python3.10 python3.10-venv python3.10-distutils python3-pip ffmpeg && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Upgrade pip and install yt-dlp
RUN python3.10 -m ensurepip && \
    python3.10 -m pip install --upgrade pip && \
    python3.10 -m pip install --no-cache-dir yt-dlp

# Copy the built jar
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
