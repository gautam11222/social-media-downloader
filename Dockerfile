# Use Python 3.11 slim as base
FROM python:3.11-slim

# Install Java 17, Maven, ffmpeg, curl
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    maven \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build Java project without running tests
RUN mvn clean package -DskipTests

# Install yt-dlp
RUN pip install --no-cache-dir yt-dlp

# Default command (optional, change as per your app)
CMD ["java", "-jar", "target/your-app.jar"]
