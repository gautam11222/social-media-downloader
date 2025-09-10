# ==========================
# Stage 1: Build the Spring Boot application using Maven
# ==========================
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application (skip tests)
RUN mvn clean package -DskipTests

# ==========================
# Stage 2: Runtime image with OpenJDK 17 + Python 3.10 + yt-dlp + ffmpeg
# ==========================
FROM ubuntu:22.04

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

# Install dependencies: Python 3.10, pip, ffmpeg, curl, certificates, gnupg, software-properties-common
RUN apt-get update && \
    apt-get install -y software-properties-common gnupg curl ca-certificates && \
    add-apt-repository ppa:deadsnakes/ppa && \
    apt-get update && \
    apt-get install -y \
        python3.10 \
        python3.10-venv \
        python3.10-distutils \
        python3-pip \
        ffmpeg && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Upgrade pip and install yt-dlp
RUN python3.10 -m pip install --no-cache-dir --upgrade pip yt-dlp

# Copy the built jar file from the builder stage
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port (Render assigns PORT dynamically)
EXPOSE 8080

# Use PORT environment variable for Spring Boot (default 8080)
ENV PORT 8080
ENV JAVA_TOOL_OPTIONS="-Dserver.port=${PORT}"

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
