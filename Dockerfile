# ============================
# Build Stage
# ============================
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first for caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the Spring Boot app
RUN ./mvnw clean package -DskipTests

# ============================
# Runtime Stage
# ============================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install system dependencies
# - ffmpeg → required for video/audio processing
# - clamav → for virus scanning
# - python3 + pip → required for yt-dlp
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg curl clamav ca-certificates python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp (latest version)
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp \
    -o /usr/local/bin/yt-dlp \
    && chmod +x /usr/local/bin/yt-dlp

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the app port
EXPOSE 8080

# Run the app
CMD ["java", "-jar", "app.jar"]
