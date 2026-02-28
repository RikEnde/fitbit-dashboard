#!/bin/bash
set -e

: "${GITHUB_USER:?GITHUB_USER environment variable is not set}"

REGISTRY="ghcr.io/${GITHUB_USER}"
IMAGE="fitbit"
TAG="${1:-latest}"
FULL_IMAGE="$REGISTRY/$IMAGE:$TAG"

echo "Building and pushing $FULL_IMAGE for linux/amd64..."

# Ensure a buildx builder with multi-platform support exists
if ! docker buildx inspect multibuilder &>/dev/null; then
    echo "Creating buildx builder..."
    docker buildx create --name multibuilder --use
else
    docker buildx use multibuilder
fi

docker buildx build \
    --platform linux/amd64 \
    -t "$FULL_IMAGE" \
    --push \
    "$(dirname "$0")/.."

echo ""
echo "Done. Pushed: $FULL_IMAGE"
echo "View at: https://github.com/${GITHUB_USER}?tab=packages"
