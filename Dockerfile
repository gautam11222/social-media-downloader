# ---------- BUILD STAGE (Java build) ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the Java project (skip tests)
RUN mvn clean package -DskipTests

# ---------- RUNTIME STAGE (Python + Java) ----------
FROM python:3.10-slim

# Set working directory
WORKDIR /app

# Install Java runtime, ffmpeg, curl, Python venv tools
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk-headless \
    ffmpeg \
    curl \
    python3-venv \
    python3-distutils \
    && rm -rf /var/lib/apt/lists/*

# Copy the built Java JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts
COPY ./*.py ./

# Optional: install Python packages if requirements.txt exists
COPY requirements.txt ./ 2>/dev/null || true
RUN if [ -f requirements.txt ]; then pip install --no-cache-dir -r requirements.txt; fi

# Install yt-dlp globally
RUN pip install --no-cache-dir yt-dlp

# Expose port if your app needs (example 8080)
EXPOSE 8080

# Default command: run Python script (replace with your main script)
CMD ["python3", "main.py"]
