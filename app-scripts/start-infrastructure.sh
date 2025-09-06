#!/bin/bash

# Store Application Infrastructure Startup Script
# This script starts all required infrastructure services for the Store Application

set -e

echo "🚀 Starting Store Application Infrastructure..."
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    print_status "Checking Docker status..."
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker Desktop and try again."
        exit 1
    fi
    print_success "Docker is running"
}

# Function to start PostgreSQL
start_postgres() {
    print_status "Starting PostgreSQL database..."
    
    # Check if postgres container already exists
    if docker ps -a --format 'table {{.Names}}' | grep -q '^postgres$'; then
        print_status "PostgreSQL container already exists. Starting..."
        docker start postgres > /dev/null 2>&1
    else
        print_status "Creating new PostgreSQL container..."
        docker run -d \
            --name postgres \
            --restart always \
            -e POSTGRES_USER=admin \
            -e POSTGRES_PASSWORD=admin \
            -e POSTGRES_DB=store \
            -v postgres_data:/var/lib/postgresql/data \
            -p 5432:5432 \
            postgres:16.2 \
            postgres -c wal_level=logical > /dev/null 2>&1
    fi
    
    # Wait for PostgreSQL to be ready
    print_status "Waiting for PostgreSQL to be ready..."
    local retry_count=0
    local max_retries=30
    
    while [ $retry_count -lt $max_retries ]; do
        if docker exec postgres pg_isready -U admin -d store > /dev/null 2>&1; then
            print_success "PostgreSQL is ready and accepting connections"
            break
        fi
        sleep 1
        retry_count=$((retry_count + 1))
    done
    
    if [ $retry_count -eq $max_retries ]; then
        print_error "PostgreSQL failed to start within 30 seconds"
        exit 1
    fi
}

# Function to start Redis
start_redis() {
    print_status "Starting Redis cache..."
    
    # Check if redis container already exists
    if docker ps -a --format 'table {{.Names}}' | grep -q '^redis$'; then
        print_status "Redis container already exists. Starting..."
        docker start redis > /dev/null 2>&1
    else
        print_status "Creating new Redis container..."
        docker run -d \
            --name redis \
            --restart always \
            -p 6379:6379 \
            -v redis_data:/data \
            redis:7-alpine \
            redis-server --appendonly yes > /dev/null 2>&1
    fi
    
    # Wait for Redis to be ready
    print_status "Waiting for Redis to be ready..."
    local retry_count=0
    local max_retries=30
    
    while [ $retry_count -lt $max_retries ]; do
        if docker exec redis redis-cli ping | grep -q PONG > /dev/null 2>&1; then
            print_success "Redis is ready and accepting connections"
            break
        fi
        sleep 1
        retry_count=$((retry_count + 1))
    done
    
    if [ $retry_count -eq $max_retries ]; then
        print_error "Redis failed to start within 30 seconds"
        exit 1
    fi
}

# Function to verify services
verify_services() {
    print_status "Verifying infrastructure services..."
    echo ""
    
    # Check PostgreSQL
    if docker exec postgres pg_isready -U admin -d store > /dev/null 2>&1; then
        print_success "✅ PostgreSQL: Running and healthy"
        echo "   📍 Connection: jdbc:postgresql://localhost:5432/store"
        echo "   👤 Username: admin"
        echo "   🔒 Password: admin"
    else
        print_error "❌ PostgreSQL: Not responding"
    fi
    
    # Check Redis
    if docker exec redis redis-cli ping | grep -q PONG > /dev/null 2>&1; then
        print_success "✅ Redis: Running and healthy"
        echo "   📍 Connection: localhost:6379"
        echo "   🔧 Configuration: Persistent storage enabled"
    else
        print_error "❌ Redis: Not responding"
    fi
    
    echo ""
    print_success "Infrastructure services are ready!"
}

# Function to show next steps
show_next_steps() {
    echo ""
    echo "🎯 Next Steps:"
    echo "=============="
    echo "1. Start the Spring Boot application:"
    echo "   ./gradlew bootRun"
    echo ""
    echo "2. Or start with custom database port if needed:"
    echo "   ./gradlew bootRun --args='--spring.datasource.url=jdbc:postgresql://localhost:5432/store'"
    echo ""
    echo "3. Access the application:"
    echo "   🌐 Application: http://localhost:8081"
    echo "   📚 Swagger UI: http://localhost:8081/swagger-ui.html"
    echo "   🔍 API Docs: http://localhost:8081/v3/api-docs"
    echo "   ❤️  Health Check: http://localhost:8081/actuator/health"
    echo ""
    echo "4. Test API endpoints:"
    echo "   curl -X GET http://localhost:8081/api/v1/customers"
    echo "   curl -X POST http://localhost:8081/api/v1/customers \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"name\": \"Test Customer\"}'"
    echo ""
}

# Function to handle script interruption
cleanup() {
    print_warning "Script interrupted. Infrastructure services will continue running."
    print_status "To stop services manually, run:"
    echo "   docker stop postgres redis"
    exit 1
}

# Set up signal handling
trap cleanup SIGINT SIGTERM

# Main execution
main() {
    echo "🏪 Store Application Infrastructure Setup"
    echo "========================================"
    echo ""
    
    check_docker
    start_postgres
    start_redis
    verify_services
    show_next_steps
    
    print_success "Infrastructure setup complete! 🎉"
}

# Execute main function
main "$@"