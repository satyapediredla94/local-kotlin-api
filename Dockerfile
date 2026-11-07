# --- Stage 1: Build the application ---
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
# Build a shadow/fat JAR containing all dependencies
./gradlew installDist/

# --- Stage 2: Run the application ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built distribution from the builder stage
COPY --from=builder /app/build/install/local-kotlin-api /app

EXPOSE 3000
# Run the generated start script
CMD ["./bin/local-kotlin-api"]