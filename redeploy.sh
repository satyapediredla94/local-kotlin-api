#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

CONTAINER_NAME="quickbite-app"
IMAGE_NAME="local-kotlin-api"
PORT_MAPPING="3000:3000"
HOST_DATA_DIR="$(pwd)/data"
CONTAINER_DATA_DIR="/app/data"

echo "=== Step 1: Stopping existing container (if running) ==="
docker stop "$CONTAINER_NAME" 2>/dev/null || echo "Container was not running."

echo "=== Step 2: Removing old container ==="
docker rm "$CONTAINER_NAME" 2>/dev/null || echo "No old container to remove."

echo "=== Step 3: Rebuilding Docker image ==="
docker build -t "$IMAGE_NAME" .

echo "=== Step 4: Starting new container with persistent volume ==="
docker run -d \
  -p "$PORT_MAPPING" \
  -v "$HOST_DATA_DIR":"$CONTAINER_DATA_DIR" \
  --name "$CONTAINER_NAME" \
  "$IMAGE_NAME"

echo "==== Deployment Complete! ===="
docker ps