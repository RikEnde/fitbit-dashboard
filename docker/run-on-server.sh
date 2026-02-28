#!/bin/bash
set -e

: "${GITHUB_USER:?GITHUB_USER environment variable is not set}"

REGISTRY="ghcr.io/${GITHUB_USER}"
IMAGE="fitbit"
TAG="${1:-latest}"
FULL_IMAGE="$REGISTRY/$IMAGE:$TAG"

CONTAINER_NAME="fitbit"
PG_VOLUME="fitbit-pgdata"
PORT="${PORT:-8080}"

echo "Pulling $FULL_IMAGE..."
docker pull "$FULL_IMAGE"

# Stop and remove existing container if running
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Stopping existing container..."
    docker rm -f "$CONTAINER_NAME"
fi

echo "Starting container..."
docker run -d \
    --name "$CONTAINER_NAME" \
    --restart unless-stopped \
    -p "$PORT":8080 \
    -v "$PG_VOLUME":/var/lib/postgresql/data \
    -e POSTGRES_USER="${POSTGRES_USER:-fitbit}" \
    -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}" \
    -e POSTGRES_DB="${POSTGRES_DB:-fitbit}" \
    "$FULL_IMAGE"

echo ""
echo "Container started. Waiting for Spring Boot..."
sleep 20
docker logs "$CONTAINER_NAME" 2>&1 | grep -E "(Started|ERROR)" | head -5

echo ""
echo "Running at http://localhost:$PORT"
echo "To follow logs: docker logs -f $CONTAINER_NAME"
echo "To run importer: docker exec $CONTAINER_NAME java -jar /app/importer-cli.jar --all --user=NAME --datadir=/data"
