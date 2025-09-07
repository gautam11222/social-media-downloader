#!/bin/bash

set -e

echo "🚀 Starting Social Media Downloader..."

# Start clamav daemon
if command -v clamd >/dev/null 2>&1; then
    echo "📡 Starting ClamAV daemon..."
    clamd &
    sleep 2
else
    echo "⚠️  ClamAV not found, continuing without virus scanning..."
fi

# Update ClamAV database
echo "🔄 Updating virus definitions..."
freshclam --quiet || echo "⚠️  Failed to update ClamAV database, continuing..."

# Create and setup temp directory
mkdir -p /tmp/socialdownloader
chmod 755 /tmp/socialdownloader

# Verify yt-dlp installation
echo "✅ Checking yt-dlp installation..."
yt-dlp --version && echo "✅ yt-dlp ready!" || echo "❌ yt-dlp not found"

# Set JVM options for containers
export JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Start Spring Boot application
echo "🌟 Starting Spring Boot application on port 8080..."
exec java $JAVA_OPTS -jar app.jar
