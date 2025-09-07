#!/bin/bash

set -e

# Start clamav daemon (if available)
if command -v clamd >/dev/null 2>&1; then
    echo "Starting clamav-daemon..."
    clamd &
else
    echo "clamd not found, continuing without virus scanning..."
fi

# Update ClamAV database
echo "Updating virus definitions..."
freshclam --quiet || echo "Failed to update ClamAV database, continuing..."

# Create temp directory
mkdir -p /tmp/socialdownloader

# Set proper permissions
chmod 755 /tmp/socialdownloader

# Check yt-dlp installation
echo "Checking yt-dlp installation..."
yt-dlp --version || echo "Warning: yt-dlp not found"

# Start the Spring Boot application
echo "Starting Social Downloader application..."
exec java $JAVA_OPTS -jar socialdownloader.jar
