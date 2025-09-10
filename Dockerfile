FROM openjdk:17-jdk-slim

ENV APP_HOME=/app

WORKDIR ${APP_HOME}

# Install dependencies: curl, ffmpeg and yt-dlp pinned to version 2023.10.10
RUN apt-get update && \
    apt-get install -y ffmpeg curl && \
    curl -L https://github.com/yt-dlp/yt-dlp/releases/download/2023.10.10/yt-dlp -o /usr/local/bin/yt-dlp && \
    chmod a+rx /usr/local/bin/yt-dlp && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy the built jar file into the container
COPY target/downloader-0.0.1-SNAPSHOT.jar ${APP_HOME}/app.jar

# Copy application.properties if needed
COPY src/main/resources/application.properties ${APP_HOME}/application.properties

# Expose port 8080
EXPOSE 8080

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
