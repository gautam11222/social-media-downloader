# ================================
# Build stage (Java + Maven)
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom and source
COPY pom.xml .
COPY src ./src

# Build Java app
RUN mvn clean package -DskipTests

# ================================
# Runtime stage (Python + Java)
# ================================
FROM python:3.10-slim
WORKDIR /app

# Install dependencies: Java runtime (17), ffmpeg, curl, python venv
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    ffmpeg \
    curl \
    python3-venv \
    && rm -rf /var/lib/apt/lists/*

# Copy Java app from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts (if any)
COPY ./*.py ./ 

# Copy requirements.txt and install dependencies (safe if empty)
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt || true

# Expose default port (Render overrides with $PORT)
EXPOSE 8080

# Run Java app
CMD ["java", "-jar", "app.jar"]
