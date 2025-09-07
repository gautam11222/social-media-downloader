# ========================
# Stage 1: Build the app
# ========================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven files and source
COPY pom.xml .
COPY src ./src

# Build Spring Boot fat jar (with repackage)
RUN mvn -q clean package -DskipTests

# ========================
# Stage 2: Runtime
# ========================
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Install python3 (needed for yt-dlp), ffmpeg, clamav
RUN apt-get update && apt-get install -y --no-install-recommends \
      wget \
      ffmpeg \
      ca-certificates \
      clamav \
      python3 && \
    rm -rf /var/lib/apt/lists/*

# Install latest yt-dlp
RUN wget -O /usr/local/bin/yt-dlp https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp && \
    chmod +x /usr/local/bin/yt-dlp

# Copy built jar from build stage
COPY --from=build /app/target/*.jar /app/app.jar

# Update ClamAV definitions (ignore errors if offline)
RUN freshclam || true

# Expose port for Render
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java","-jar","/app/app.jar"]
