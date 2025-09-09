# Use Maven with JDK 17 as base (includes Maven and Java)
FROM maven:3.9.6-eclipse-temurin-17

# Set working directory
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the Java project (skip tests for faster build)
RUN mvn clean package -DskipTests

# Copy Python scripts (if any)
COPY ./*.py ./

# Install Python 3 venv and ffmpeg inside container
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3-venv \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Optional: install Python dependencies if requirements.txt exists
COPY requirements.txt ./ 2>/dev/null || true
RUN if [ -f requirements.txt ]; then pip install --no-cache-dir -r requirements.txt; fi

# Expose port if your Java app uses it
EXPOSE 8080

# Default command: run the Java JAR
CMD ["java", "-jar", "target/your-app-name.jar"]
