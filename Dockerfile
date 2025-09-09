# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Run stage ----------
FROM openjdk:17-jdk-slim
WORKDIR /app

# Install Python 3.11, pip, ffmpeg, yt-dlp
RUN apt-get update && apt-get install -y \
    python3.11 \
    python3.11-venv \
    python3.11-distutils \
    ffmpeg \
    curl \
 && curl -sS https://bootstrap.pypa.io/get-pip.py | python3.11 \
 && pip3 install --no-cache-dir yt-dlp \
 && rm -rf /var/lib/apt/lists/*

# Copy built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
