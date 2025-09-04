# Stage 1: Build the app
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with yt-dlp + ffmpeg + clamav
FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    ca-certificates \
    clamav \
    clamav-daemon \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install yt-dlp

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Update ClamAV database
RUN freshclam || true

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
