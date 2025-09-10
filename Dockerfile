# Stage 1: Build the Spring Boot app
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with Ubuntu 22.04 (Python 3.10+)
FROM ubuntu:22.04

ENV APP_HOME=/app
ENV DEBIAN_FRONTEND=noninteractive
WORKDIR ${APP_HOME}

# Install system dependencies
RUN apt-get update && \
    apt-get install -y \
        openjdk-17-jre-headless \
        python3 \
        python3-pip \
        python3-venv \
        ffmpeg \
        curl \
        wget \
        ca-certificates \
        git \
        unzip && \
    # Verify Python version (should be 3.10+)
    python3 --version && \
    # Upgrade pip and install yt-dlp
    python3 -m pip install --upgrade pip setuptools wheel && \
    python3 -m pip install --no-cache-dir yt-dlp requests urllib3 && \
    # Verify yt-dlp installation
    python3 -m yt_dlp --version && \
    # Create symbolic link for direct access
    ln -sf /usr/bin/python3 /usr/local/bin/python3 && \
    # Clean up
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin:/usr/local/bin
ENV PYTHONPATH=/usr/local/lib/python3.10/site-packages
ENV PYTHONUNBUFFERED=1

# Copy application files
COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Create necessary directories with proper permissions
RUN mkdir -p ${APP_HOME}/downloads ${APP_HOME}/temp && \
    chmod 755 ${APP_HOME}/downloads ${APP_HOME}/temp

# Set working directory permissions
RUN chown -R root:root ${APP_HOME} && \
    chmod -R 755 ${APP_HOME}

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
