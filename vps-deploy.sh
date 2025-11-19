#!/bin/bash
set -e

PROJECT_DIR="/www/wwwroot/CITSNVN/icsQuizUserService"
IMAGE_NAME="icsquiz-user-service:latest"
CONTAINER_NAME="icsquiz_user_app"
PORT=3090

echo "=========================================="
echo "🚀 Starting VPS Deployment..."
echo "=========================================="

# 1. Navigate to project directory
echo ""
echo "[1] Navigating to project directory..."
cd "$PROJECT_DIR"
pwd

# 2. Check Maven and Java
echo ""
echo "[2] Checking Java and Maven..."
java -version
mvn --version

# 3. Clean and build JAR
echo ""
echo "[3] Building JAR with Maven..."
mvn clean package -DskipTests

# 4. Check if JAR was created
echo ""
echo "[4] Verifying JAR creation..."
JAR_FILE=$(find target -name "*.jar" -type f)
if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR file not found!"
    exit 1
fi
echo "✅ JAR created: $JAR_FILE"

# 5. Fix Dockerfile line endings (if needed)
echo ""
echo "[5] Fixing Dockerfile line endings..."
if [ -f "Dockerfile" ]; then
    dos2unix Dockerfile 2>/dev/null || sed -i 's/\r$//' Dockerfile
    echo "✅ Dockerfile ready"
else
    echo "❌ Dockerfile not found!"
    exit 1
fi

# 6. Build Docker image
echo ""
echo "[6] Building Docker image..."
docker build -t $IMAGE_NAME .

# 7. Verify image was created
echo ""
echo "[7] Verifying Docker image..."
if docker images | grep -q icsquiz-user-service; then
    echo "✅ Docker image created successfully"
    docker images | grep icsquiz-user-service
else
    echo "❌ Docker image build failed!"
    exit 1
fi

# 8. Stop and remove old container
echo ""
echo "[8] Stopping old container..."
docker stop $CONTAINER_NAME 2>/dev/null || true
docker rm $CONTAINER_NAME 2>/dev/null || true
sleep 2

# 9. Start new container
echo ""
echo "[9] Starting new Docker container..."
docker run -d \
  --name $CONTAINER_NAME \
  -p $PORT:$PORT \
  --restart unless-stopped \
  --memory="512m" \
  --cpus="1" \
  $IMAGE_NAME

# 10. Verify container is running
echo ""
echo "[10] Verifying container..."
sleep 3
if docker ps | grep -q $CONTAINER_NAME; then
    echo "✅ Container is running"
    docker ps | grep $CONTAINER_NAME
else
    echo "❌ Container failed to start"
    docker logs $CONTAINER_NAME
    exit 1
fi

# 11. Display logs
echo ""
echo "[11] Application logs (last 30 lines):"
docker logs --tail 30 $CONTAINER_NAME

# 12. Final status
echo ""
echo "=========================================="
echo "✅ DEPLOYMENT COMPLETE!"
echo "=========================================="
echo "Service running at: http://$(hostname -I | awk '{print $1}'):$PORT"
echo "Container name: $CONTAINER_NAME"
echo "Docker image: $IMAGE_NAME"
echo ""
echo "Useful commands:"
echo "  View logs: docker logs -f $CONTAINER_NAME"
echo "  Stop container: docker stop $CONTAINER_NAME"
echo "  Start container: docker start $CONTAINER_NAME"
echo "  Remove image: docker rmi $IMAGE_NAME"
echo "=========================================="