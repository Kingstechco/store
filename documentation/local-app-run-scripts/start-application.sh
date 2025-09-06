#!/bin/bash

# Store Application Startup Script
# This script starts the Store Application after ensuring infrastructure is ready

set -e

echo "🏪 Starting Store Application..."
echo "================================"

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

# Function to check if infrastructure is running
check_infrastructure() {
    print_status "Checking infrastructure services..."
    
    local postgres_ready=false
    local redis_ready=false
    
    # Check PostgreSQL
    if docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -q '^postgres.*Up'; then
        if docker exec postgres pg_isready -U admin -d store > /dev/null 2>&1; then
            print_success "PostgreSQL is running and ready"
            postgres_ready=true
        else
            print_warning "PostgreSQL container is up but not ready"
        fi
    else
        print_error "PostgreSQL container is not running"
        echo "   Run: ./start-infrastructure.sh to start infrastructure services"
        return 1
    fi
    
    # Check Redis
    if docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -q '^redis.*Up'; then
        if docker exec redis redis-cli ping | grep -q PONG > /dev/null 2>&1; then
            print_success "Redis is running and ready"
            redis_ready=true
        else
            print_warning "Redis container is up but not ready"
        fi
    else
        print_error "Redis container is not running"
        echo "   Run: ./start-infrastructure.sh to start infrastructure services"
        return 1
    fi
    
    if [ "$postgres_ready" = true ] && [ "$redis_ready" = true ]; then
        return 0
    else
        return 1
    fi
}

# Function to check Java environment
check_java() {
    print_status "Checking Java environment..."
    
    if [ -z "$JAVA_HOME" ]; then
        export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
        print_status "Set JAVA_HOME to $JAVA_HOME"
    fi
    
    if ! command -v java > /dev/null 2>&1; then
        print_error "Java is not available in PATH"
        return 1
    fi
    
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -ge 17 ] 2>/dev/null; then
        print_success "Java $java_version is available"
    else
        print_error "Java 17+ is required, but found Java $java_version"
        return 1
    fi
}

# Function to stop any existing application
stop_existing_app() {
    print_status "Checking for existing application processes..."
    
    # Find and kill any existing StoreApplication processes
    local pids=$(ps aux | grep 'StoreApplication' | grep -v grep | awk '{print $2}')
    if [ ! -z "$pids" ]; then
        print_warning "Found existing application processes, stopping them..."
        echo "$pids" | xargs kill -9 > /dev/null 2>&1 || true
        sleep 2
        print_success "Stopped existing application processes"
    fi
    
    # Check if port 8081 is in use
    if lsof -i :8081 > /dev/null 2>&1; then
        print_warning "Port 8081 is in use, attempting to free it..."
        local port_pid=$(lsof -ti :8081)
        if [ ! -z "$port_pid" ]; then
            kill -9 $port_pid > /dev/null 2>&1 || true
            sleep 2
        fi
    fi
}

# Function to start the application
start_application() {
    print_status "Starting Store Application..."
    echo ""
    print_status "Application will start with the following configuration:"
    echo "   📍 Server Port: 8081"
    echo "   🗄️  Database: PostgreSQL (localhost:5432/store)"
    echo "   🔄 Cache: Redis (localhost:6379)"
    echo "   🔧 Profile: default"
    echo "   🖥️  Framework: Spring Boot 3.5.5"
    echo ""
    
    # Start the application
    print_status "Executing: ./gradlew bootRun"
    echo "=============================================="
    
    # Export JAVA_HOME and start the application
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    ./gradlew bootRun
}

# Function to handle script interruption
cleanup() {
    print_warning "Application startup interrupted."
    print_status "Infrastructure services will continue running."
    print_status "To stop all services, run: docker stop postgres redis"
    exit 1
}

# Set up signal handling
trap cleanup SIGINT SIGTERM

# Function to show application info
show_application_info() {
    echo ""
    echo "🎯 Application Endpoints:"
    echo "========================"
    echo "🌐 Main Application: http://localhost:8081"
    echo "📚 Swagger UI: http://localhost:8081/swagger-ui.html"
    echo "📖 API Documentation: http://localhost:8081/v3/api-docs"
    echo "❤️  Health Check: http://localhost:8081/actuator/health"
    echo ""
    echo "🔌 API Endpoints:"
    echo "   GET    /api/v1/customers"
    echo "   POST   /api/v1/customers"
    echo "   GET    /api/v1/customers/{id}"
    echo "   PUT    /api/v1/customers/{id}"
    echo "   DELETE /api/v1/customers/{id}"
    echo ""
    echo "   GET    /api/v1/orders"
    echo "   POST   /api/v1/orders"
    echo "   GET    /api/v1/orders/{id}"
    echo "   PUT    /api/v1/orders/{id}"
    echo "   DELETE /api/v1/orders/{id}"
    echo ""
    echo "   GET    /api/v1/products"
    echo "   POST   /api/v1/products"
    echo "   GET    /api/v1/products/{id}"
    echo "   PUT    /api/v1/products/{id}"
    echo "   DELETE /api/v1/products/{id}"
    echo ""
}

# Main execution
main() {
    echo ""
    
    # Check if user wants to see help
    if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
        echo "Store Application Startup Script"
        echo ""
        echo "Usage: $0 [options]"
        echo ""
        echo "Options:"
        echo "  --help, -h          Show this help message"
        echo "  --skip-checks       Skip infrastructure checks (not recommended)"
        echo ""
        echo "This script will:"
        echo "1. Check that PostgreSQL and Redis containers are running"
        echo "2. Verify Java 17+ is available"
        echo "3. Stop any existing application processes"
        echo "4. Start the Store Application with Spring Boot"
        echo ""
        echo "Prerequisites:"
        echo "- Docker must be running"
        echo "- Run ./start-infrastructure.sh first to start database and cache"
        echo ""
        return 0
    fi
    
    # Skip infrastructure checks if requested (for development)
    if [ "$1" != "--skip-checks" ]; then
        if ! check_infrastructure; then
            print_error "Infrastructure services are not ready"
            echo ""
            print_status "To start infrastructure services, run:"
            echo "   ./start-infrastructure.sh"
            echo ""
            exit 1
        fi
    fi
    
    if ! check_java; then
        exit 1
    fi
    
    stop_existing_app
    
    show_application_info
    
    print_success "Starting application..."
    echo ""
    
    start_application
}

# Execute main function
main "$@"