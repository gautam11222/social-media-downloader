# ----------------------
# Stage 1: Build Java App
# ----------------------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven files and source
COPY pom.xml .
COPY src ./src

# Build the project (skip tests)
RUN mvn clean package -DskipTests

# ----------------------
# Stage 2: Runtime Image
# ----------------------
FROM python:3.10-slim

# Install dependencies: Java runtime, ffmpeg, curl, venv, distutils
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk-headless \
    ffmpeg \
    curl \
    python3-venv \
    python3-distutils \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built Java JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts (if any)
COPY ./*.py ./

# Install Python packages if requirements.txt exists
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt || echo "No requirements.txt found, skipping pip install"

# Expose port if needed
EXPOSE 8080

# Default command: Run Python script (adjust if you want Java instead)
CMD ["python3", "main.py"]
# To run Java JAR instead:
# CMD ["java", "-jar", "app.jar"]
