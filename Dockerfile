# ===== Build Stage: Maven build =====
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and source
COPY pom.xml . 
COPY src ./src

# Build the Java project
RUN mvn clean package -DskipTests

# ===== Run Stage: Python + Java =====
FROM python:3.10-slim
WORKDIR /app

# Install dependencies: Java, ffmpeg, curl, Python tools
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk-headless \
    ffmpeg \
    curl \
    python3-venv \
    python3-distutils \
    && rm -rf /var/lib/apt/lists/*

# Copy Java artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts
COPY ./*.py ./

# Optional: create empty requirements.txt if missing
RUN touch requirements.txt

# Install Python packages if any
RUN if [ -s requirements.txt ]; then pip install --no-cache-dir -r requirements.txt; fi

# Expose port (if your Java app runs on 8080, adjust if needed)
EXPOSE 8080

# Command to run both Java and Python scripts
# Adjust as per your project (example: run Java JAR)
CMD ["java", "-jar", "app.jar"]
