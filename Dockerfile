# ------------------------------
# Stage 1: Build Java Application
# ------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven files
COPY pom.xml . 
COPY src ./src

# Build the project and skip tests
RUN mvn clean package -DskipTests


# ------------------------------
# Stage 2: Runtime Environment
# ------------------------------
FROM openjdk:17-slim
WORKDIR /app

# Install Python 3.10, pip, ffmpeg, curl, and clean up
RUN apt-get update && apt-get install -y \
    python3.10 \
    python3.10-venv \
    python3.10-distutils \
    ffmpeg \
    curl \
    && curl -sS https://bootstrap.pypa.io/get-pip.py | python3.10 \
    && pip3 install --no-cache-dir yt-dlp \
    && rm -rf /var/lib/apt/lists/*

# Copy the built jar from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose your app port (change if needed)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
