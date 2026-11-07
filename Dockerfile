# --- Stage 1: Build the application ---
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy Gradle files and wrapper
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Grant execute permission to gradlew
RUN chmod +x gradlew

# Copy source code and build the distribution
COPY src ./src
RUN ./gradlew installDist --no-daemon

# --- Stage 2: Run the application ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the exact built distribution from the builder stage
COPY --from=builder /app/build/install/local-kotlin-api /app

EXPOSE 3000
# Run the generated start script inside bin/
ENTRYPOINT ["/app/bin/local-kotlin-api"]