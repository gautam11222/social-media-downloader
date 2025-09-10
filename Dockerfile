# Stage 1: Build the application using Maven
FROM maven:3.9.8-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

ENV APP_HOME=/app

WORKDIR ${APP_HOME}

# Install dependencies and yt-dlp version 2023.10.10
RUN apt-get update && \
    apt-get install -y ffmpeg curl && \
    curl -L --retry 3 --retry-delay 5 https://github.com/yt-dlp/yt-dlp/releases/download/2023.10.10/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
