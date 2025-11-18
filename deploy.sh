#!/bin/bash
set -e

DEPLOY_DIR="/www/wwwroot/CITSNVN/icsQuizUserService"
JAR_FILE="icsQuizUserService-0.1.jar"
DOCKER_IMAGE="icsquiz-user-service:latest"
DOCKER_CONTAINER="icsquiz_user_app"
PORT="3090"

echo "=========================================="
echo "Starting Deployment Process"
echo "=========================================="

echo ""
echo "Step 1: Verifying files..."
if [ ! -f "$DEPLOY_DIR/Dockerfile" ]; then
    echo "ERROR: Dockerfile not found at $DEPLOY_DIR/Dockerfile"
    exit 1
fi

if [ ! -f "$DEPLOY_DIR/$JAR_FILE" ]; then
    echo "ERROR: JAR file not found at $DEPLOY_DIR/$JAR_FILE"
    exit 1
fi

echo "✓ Dockerfile found"
echo "✓ JAR file found"

echo ""
echo "Step 2: Fixing file permissions..."
chmod 644 "$DEPLOY_DIR/Dockerfile"
chmod 644 "$DEPLOY_DIR/$JAR_FILE"
echo "✓ Permissions updated"

echo ""
echo "Step 3: Building Docker image..."
cd "$DEPLOY_DIR"

echo "Dockerfile content:"
cat Dockerfile
echo ""

docker build -t "$DOCKER_IMAGE" .
echo "✓ Docker image built successfully"

echo ""
echo "Step 4: Stopping old container..."
docker stop "$DOCKER_CONTAINER" || true
docker rm "$DOCKER_CONTAINER" || true
echo "✓ Old container stopped/removed"

echo ""
echo "Step 5: Starting new container..."
docker run -d \
    --name "$DOCKER_CONTAINER" \
    -p "$PORT:3090" \
    --restart unless-stopped \
    "$DOCKER_IMAGE"

echo "✓ New container started"

echo ""
echo "Step 6: Verifying deployment..."
sleep 2
if docker ps | grep -q "$DOCKER_CONTAINER"; then
    echo "✓ Container is running!"
    echo ""
    docker ps | grep "$DOCKER_CONTAINER"
else
    echo "ERROR: Container failed to start"
    docker logs "$DOCKER_CONTAINER" || true
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ Deployment completed successfully!"
echo "=========================================="
echo "Service running on port $PORT"
echo ""