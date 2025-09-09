#!/bin/bash
set -e
# Start clamav daemon
if command -v clamd >/dev/null 2>&1; then
  echo "Starting clamav-daemon..."
  /etc/init.d/clamav-daemon start || true
else
  echo "clamd not found, continuing..."
fi
# Ensure freshclam run periodically
freshclam || true
# Create temp dir
mkdir -p /tmp/socialdownloader
# Run app
exec java -jar /app/socialdownloader.jar
