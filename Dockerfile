# Multi-stage Dockerfile for building and running the Spring Boot app on Render

### Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies first (better caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

### Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install dependencies: ffmpeg, yt-dlp, clamav
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg curl clamav ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod +x /usr/local/bin/yt-dlp

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
