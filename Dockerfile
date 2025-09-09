# Multi-stage Dockerfile for building and running the Spring Boot app on Render
### Build stage
FROM maven:3.9.3-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -B -DskipTests package -DskipITs

### Run stage (extended with ffmpeg + yt-dlp)
FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

# Install runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends     openjdk-17-jre-headless     ffmpeg     python3     python3-pip     curl     ca-certificates     clamav     clamav-daemon     && rm -rf /var/lib/apt/lists/*

# Install yt-dlp
RUN python3 -m pip install --no-cache-dir yt-dlp

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Optional: update clamav db (ignore errors)
RUN freshclam || true

ENV PORT 8080
EXPOSE 8080

ENTRYPOINT ["sh","-c","exec java -Dserver.port=${PORT:-8080} -Xms256m -Xmx512m -jar /app/app.jar"]
