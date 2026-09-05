#!/bin/bash

# Docker Build Script for Ecommerce Services (Java / Quarkus / gRPC)
# This script builds Docker images for all services.
#
# Image names match what the Kubernetes manifests reference, e.g.
# `ghcr.io/<owner>/quarkus-ecommerce/auth:latest` (owner overridable with
# the GHCR_OWNER env var), so kustomize deployments pick up local images
# with `imagePullPolicy: IfNotPresent`.

set -e

echo "🐳 Building Docker images for Ecommerce Quarkus services..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if docker is installed and running
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! docker info &> /dev/null; then
        print_error "Docker is not running. Please start Docker."
        exit 1
    fi
    print_status "Docker is installed and running"
}

# Build Docker image for a service.
#   $1 = source directory (module dir, e.g. "gateway")
#   $2 = image basename referenced by k8s manifests (e.g. "apigateway")
build_service_image() {
    local service_dir="$1"
    local image_basename="$2"

    if [ -z "$service_dir" ] || [ -z "$image_basename" ]; then
        print_error "build_service_image requires <dir> and <image-basename>"
        return 1
    fi

    local dockerfile="${service_dir}/Dockerfile"
    local image="ghcr.io/${GHCR_OWNER:-mamangrust}/quarkus-ecommerce/${image_basename}:latest"

    print_status "Building ${image} (from ${service_dir})..."

    if [ ! -d "$service_dir" ]; then
        print_error "Service directory not found: $service_dir"
        return 1
    fi

    if [ ! -f "$dockerfile" ]; then
        print_error "Dockerfile not found: $dockerfile"
        return 1
    fi

    if [ ! -f "${service_dir}/pom.xml" ]; then
        print_error "pom.xml not found in service: $service_dir"
        return 1
    fi

    if docker build \
        --progress=plain \
        -f "$dockerfile" \
        -t "$image" \
        .; then

        print_status "Successfully built ${image}"
        return 0
    else
        print_error "Failed to build ${image}"
        return 1
    fi
}

# Build all service images
build_all_images() {
    print_status "Building all service images..."

    # <module-dir>:<image-basename> — basename must match the k8s manifests.
    services=(
        "gateway:apigateway"
        "auth:auth"
        "role:role"
        "user:user"
        "email-service:email"
        "banner:banner"
        "cart:cart"
        "category:category"
        "merchant:merchant"
        "merchant_award:merchant_award"
        "merchant_business:merchant_business"
        "merchant_detail:merchant_detail"
        "merchant_policy:merchant_policy"
        "order:order"
        "order_item:order_item"
        "product:product"
        "review:review"
        "review_detail:review_detail"
        "shipping_address:shipping_address"
        "slider:slider"
        "transaction:transaction"
        "db-migration:migrate"
    )

    local failed_builds=0

    for entry in "${services[@]}"; do
        local service_dir="${entry%%:*}"
        local image_basename="${entry##*:}"
        if ! build_service_image "$service_dir" "$image_basename"; then
            ((failed_builds++))
        fi
    done

    if [ $failed_builds -eq 0 ]; then
        print_status "🎉 All images built successfully!"
    else
        print_warning "⚠️  $failed_builds images failed to build"
        return 1
    fi
}

# Show built images
show_built_images() {
    print_status "Built Docker images:"
    docker images | grep -E "quarkus-ecommerce" | head -25
    echo ""
}

# Cleanup function
cleanup() {
    print_status "Build process completed"
}

# Main execution
main() {
    # Set trap for cleanup
    trap cleanup EXIT

    # Run checks
    check_docker

    # Build all images
    build_all_images

    # Show built images
    show_built_images

    print_status "Docker build process completed! 🎉"
}

# Handle script interruption
trap 'print_error "Build interrupted"; exit 1' INT

# Run main function
main "$@"
