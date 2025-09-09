# Stage 1: Build Java app
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:17-slim

WORKDIR /app

# Install Python 3.11, pip, ffmpeg, curl
RUN apt-get update && apt-get install -y \
    python3.11 \
    python3.11-venv \
    python3.11-distutils \
    ffmpeg \
    curl \
    && curl -sS https://bootstrap.pypa.io/get-pip.py | python3.11 \
    && pip3 install --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/*

# Copy JAR from build stage
COPY --from=build /app/target/*.jar ./app.jar

CMD ["java", "-jar", "app.jar"]
