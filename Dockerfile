# Use official Maven image to build the application
FROM maven:3.9.3-eclipse-temurin-17 as builder

WORKDIR /app

# Copy Maven wrapper and configs first
COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY pom.xml pom.xml

# Copy source code
COPY src src

# Ensure mvnw is executable (fixes permission issues)
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Use official OpenJDK runtime as base for the run stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install necessary dependencies
RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    python3 \
    python3-pip \
    bash \
    clamav \
    clamav-daemon \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp via pip
RUN pip3 install --no-cache-dir yt-dlp

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Copy the entrypoint script
COPY entrypoint.sh entrypoint.sh

# Make entrypoint script executable
RUN chmod +x entrypoint.sh

# Create temp directory
RUN mkdir -p /tmp/socialdownloader

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["/bin/bash", "./entrypoint.sh"]
