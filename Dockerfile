# Stage 1: Build
FROM maven:3.9.3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build with Maven directly (no wrapper needed)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    python3 \
    python3-pip \
    bash \
    clamav \
    clamav-daemon \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp with system package override
RUN pip3 install --no-cache-dir yt-dlp --break-system-packages

# Copy built jar
COPY --from=builder /app/target/*.jar app.jar

# Copy entrypoint
COPY entrypoint.sh ./
RUN chmod +x entrypoint.sh

# Create temp directory
RUN mkdir -p /tmp/socialdownloader

EXPOSE 8080

ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
