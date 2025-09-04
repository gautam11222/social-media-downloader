# Docker setup

Build the jar first:

```
mvn clean package
```

Then build Docker image:

```
docker build -t socialdownloader:latest .
```

Run (exposing port 8080):

```
docker run --rm -p 8080:8080 --name social-downloader socialdownloader:latest
```

This image includes `yt-dlp`, `ffmpeg` and `clamav`. ClamAV database is updated at build/start (freshclam). The container runs `clamav-daemon` and the Spring Boot app.

Make sure you monitor ClamAV updates and container disk usage.
