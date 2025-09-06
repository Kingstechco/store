#!/bin/bash

# Store Application Docker Build Script
# This script builds and optionally runs the Store application using Docker

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
BUILD_ONLY=false
RUN_TESTS=true
PUSH_IMAGE=false
TAG="latest"
REGISTRY=""

# Help function
show_help() {
    cat << EOF
Store Application Docker Build Script

Usage: $0 [OPTIONS]

OPTIONS:
    -h, --help              Show this help message
    -b, --build-only        Only build the image, don't start services
    -t, --tag TAG           Docker image tag (default: latest)
    -r, --registry REGISTRY Registry to push to (e.g., ghcr.io/username)
    -p, --push              Push image to registry after building
    --no-tests              Skip running tests before building
    --clean                 Clean build (remove existing containers and volumes)

EXAMPLES:
    $0                                  # Build and start all services
    $0 --build-only                     # Only build the Docker image
    $0 --tag v1.0.0 --push              # Build, tag as v1.0.0, and push to registry
    $0 --registry ghcr.io/myuser --push # Build and push to GitHub Container Registry

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -b|--build-only)
            BUILD_ONLY=true
            shift
            ;;
        -t|--tag)
            TAG="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_IMAGE=true
            shift
            ;;
        --no-tests)
            RUN_TESTS=false
            shift
            ;;
        --clean)
            echo -e "${YELLOW}Cleaning up existing containers and volumes...${NC}"
            docker-compose down -v --remove-orphans 2>/dev/null || true
            docker system prune -f 2>/dev/null || true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            show_help
            exit 1
            ;;
    esac
done

# Set image name
if [[ -n "$REGISTRY" ]]; then
    IMAGE_NAME="$REGISTRY/store-app:$TAG"
else
    IMAGE_NAME="store-app:$TAG"
fi

echo -e "${GREEN}🚀 Starting Store Application Build Process${NC}"
echo -e "${GREEN}Image: $IMAGE_NAME${NC}"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker and try again.${NC}"
    exit 1
fi

# Run tests if requested
if [[ "$RUN_TESTS" == true ]]; then
    echo -e "${YELLOW}🧪 Running tests...${NC}"
    if ./gradlew test; then
        echo -e "${GREEN}✅ Tests passed${NC}"
    else
        echo -e "${RED}❌ Tests failed. Aborting build.${NC}"
        exit 1
    fi
fi

# Build the application
echo -e "${YELLOW}🔨 Building application...${NC}"
if ./gradlew build -x test; then
    echo -e "${GREEN}✅ Application built successfully${NC}"
else
    echo -e "${RED}❌ Application build failed${NC}"
    exit 1
fi

# Build Docker image
echo -e "${YELLOW}🐳 Building Docker image: $IMAGE_NAME${NC}"
if docker build -t "$IMAGE_NAME" .; then
    echo -e "${GREEN}✅ Docker image built successfully${NC}"
else
    echo -e "${RED}❌ Docker image build failed${NC}"
    exit 1
fi

# Push image if requested
if [[ "$PUSH_IMAGE" == true ]]; then
    if [[ -z "$REGISTRY" ]]; then
        echo -e "${RED}❌ Registry not specified. Use --registry option.${NC}"
        exit 1
    fi
    
    echo -e "${YELLOW}📤 Pushing image to registry...${NC}"
    if docker push "$IMAGE_NAME"; then
        echo -e "${GREEN}✅ Image pushed successfully${NC}"
    else
        echo -e "${RED}❌ Failed to push image${NC}"
        exit 1
    fi
fi

# Start services if not build-only
if [[ "$BUILD_ONLY" == false ]]; then
    echo -e "${YELLOW}🚀 Starting services with Docker Compose...${NC}"
    
    # Update image name in docker-compose if using custom registry/tag
    if [[ "$IMAGE_NAME" != "store-app:latest" ]]; then
        export STORE_IMAGE="$IMAGE_NAME"
        docker-compose up -d --remove-orphans
    else
        docker-compose up -d --remove-orphans
    fi
    
    echo -e "${GREEN}🎉 Services started successfully!${NC}"
    echo ""
    echo -e "${GREEN}📋 Service URLs:${NC}"
    echo -e "  🌐 Store API: http://localhost:8081"
    echo -e "  📊 Prometheus: http://localhost:9090"
    echo -e "  📈 Grafana: http://localhost:3000 (admin/admin123)"
    echo ""
    echo -e "${YELLOW}📱 Check service health:${NC}"
    echo -e "  docker-compose ps"
    echo -e "  curl http://localhost:8081/actuator/health"
    echo ""
    echo -e "${YELLOW}📜 View logs:${NC}"
    echo -e "  docker-compose logs -f store-app"
else
    echo -e "${GREEN}🎉 Build completed successfully!${NC}"
    echo -e "${GREEN}Image: $IMAGE_NAME${NC}"
fi