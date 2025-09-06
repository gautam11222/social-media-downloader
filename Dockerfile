# ---------- build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
      wget ffmpeg ca-certificates clamav && \
    rm -rf /var/lib/apt/lists/*
RUN wget -O /usr/local/bin/yt-dlp https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp && \
    chmod +x /usr/local/bin/yt-dlp

COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
