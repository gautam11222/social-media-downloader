# Stage 1: Build the Spring Boot app
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM openjdk:17-slim

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Install Python, pip, ffmpeg, curl, certificates
RUN apt-get update && \
    apt-get install -y \
        python3 python3-pip ffmpeg curl ca-certificates \
        git wget gnupg software-properties-common && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Upgrade pip and install yt-dlp
RUN python3 -m pip install --no-cache-dir --upgrade pip yt-dlp

# Copy the built JAR
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
