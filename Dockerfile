# Use official OpenJDK image for Java 17 (Render supports it well)
FROM eclipse-temurin:17-jdk-alpine as build

# Set work directory
WORKDIR /app

# Copy Maven wrapper & pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (helps with build cache)
RUN ./mvnw dependency:go-offline -B

# Copy source
COPY src src

# Build JAR
RUN ./mvnw clean package -DskipTests

# -------------------
# Runtime container
# -------------------
FROM eclipse-temurin:17-jre-alpine

# Install yt-dlp & ffmpeg (needed for downloads)
RUN apk add --no-cache ffmpeg curl python3 py3-pip && \
    pip install --no-cache-dir yt-dlp

WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render uses PORT env var)
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java","-jar","app.jar"]
