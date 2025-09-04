FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies: openjdk, ffmpeg, python3, pip, clamav
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jre-headless \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    ca-certificates \
    clamav \
    clamav-daemon \
    && rm -rf /var/lib/apt/lists/*

# Install yt-dlp
RUN pip3 install yt-dlp

# Copy app jar
WORKDIR /app
COPY target/socialdownloader-1.0.0.jar /app/socialdownloader.jar

# Update ClamAV database
RUN freshclam || true

# Add entrypoint script
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080
CMD ["/app/entrypoint.sh"]
