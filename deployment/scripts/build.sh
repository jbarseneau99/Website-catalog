#!/bin/bash

# Exit on error
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Root directory
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Print with color
print_status() {
    echo -e "${GREEN}[BUILD] $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

print_error() {
    echo -e "${RED}[ERROR] $1${NC}"
}

print_info() {
    echo -e "${BLUE}[INFO] $1${NC}"
}

# Process Management
cleanup_processes() {
    print_status "Running comprehensive cleanup..."
    
    # Run the enhanced cleanup script
    if [ -f "./cleanup.sh" ]; then
        chmod +x ./cleanup.sh
        ./cleanup.sh
    else
        print_error "cleanup.sh not found. Creating it..."
        cat > cleanup.sh << 'EOF'
#!/bin/bash

echo "Stopping all Docker containers..."
docker-compose down --volumes --remove-orphans

echo "Cleaning up any Kubernetes resources..."
kubectl delete deployment --all 2>/dev/null || true
kubectl delete service --all 2>/dev/null || true
kubectl delete pod --all 2>/dev/null || true

echo "Stopping any running Java processes (excluding IDE processes)..."
ps aux | grep java | grep -v "redhat.java" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

echo "Stopping Node.js and UI processes..."
pkill -f "node.*react-scripts" || true
pkill -f "webpack" || true
pkill -f "node" || true
lsof -ti:3000 | xargs kill -9 2>/dev/null || true
lsof -ti:3001 | xargs kill -9 2>/dev/null || true

echo "Pruning Docker system..."
docker system prune -f

# Clean up any lingering npm processes
echo "Cleaning up npm processes..."
ps aux | grep "npm start" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null || true

echo "Cleanup complete!"
EOF
        chmod +x ./cleanup.sh
        ./cleanup.sh
    fi
    
    # Additional safety checks
    print_info "Verifying cleanup..."
    
    # Check if any relevant ports are still in use
    if lsof -ti:3000,3001,8080,8761 > /dev/null 2>&1; then
        print_warning "Some ports are still in use. Forcing cleanup..."
        lsof -ti:3000,3001,8080,8761 | xargs kill -9 2>/dev/null || true
    fi
    
    # Give processes time to fully terminate
    sleep 2
}

# Load environment variables
load_env_files() {
    print_status "Loading environment variables..."
    
    # Create config directory if it doesn't exist
    mkdir -p config/services
    
    # Check for .env file and create from template if needed
    if [ ! -f ".env" ] && [ -f "config/env.template" ]; then
        print_warning ".env file not found - copying from config/env.template"
        cp config/env.template .env
        print_warning "Please edit .env file with your specific configuration"
    fi
    
    # Load .env file if it exists
    if [ -f ".env" ]; then
        print_info "Loading .env file"
        set -a
        source .env
        set +a
    else
        print_warning ".env file not found and no template available"
        exit 1
    fi
    
    # Load mongodb.env if it exists
    if [ ! -f "mongodb.env" ] && [ -f "config/mongodb.template" ]; then
        print_warning "mongodb.env file not found - copying from config/mongodb.template"
        cp config/mongodb.template mongodb.env
        print_warning "Please edit mongodb.env file with your specific configuration"
    fi
    
    if [ -f "mongodb.env" ]; then
        print_info "Loading mongodb.env file"
        source mongodb.env
    fi
}

# Check requirements
check_requirements() {
    print_status "Checking build requirements..."
    
    if ! command -v java >/dev/null 2>&1; then
        print_error "Java is not installed"
        exit 1
    fi
    
    if ! command -v mvn >/dev/null 2>&1; then
        print_error "Maven is not installed"
        exit 1
    fi
    
    if ! command -v node >/dev/null 2>&1; then
        print_warning "Node.js is not installed - will skip UI build"
    fi
    
    if ! command -v docker >/dev/null 2>&1; then
        print_warning "Docker is not installed - will skip Docker builds"
    fi
}

# Function to build a service
build_service() {
    local service_dir=$1
    local service_name=$2
    
    echo -e "${YELLOW}Building ${service_name}...${NC}"
    
    cd "${ROOT_DIR}/${service_dir}" || exit
    
    if [ -f "pom.xml" ]; then
        # Maven build
        mvn clean package -DskipTests
    elif [ -f "package.json" ]; then
        # Node.js build
        npm install && npm run build
    else
        echo -e "${RED}Unknown build type for ${service_name}${NC}"
        return 1
    fi
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Successfully built ${service_name}${NC}"
    else
        echo -e "${RED}Failed to build ${service_name}${NC}"
        exit 1
    fi
}

# Build infrastructure services
echo -e "${YELLOW}Building Infrastructure Services...${NC}"
build_service "infrastructure/service-discovery" "Service Discovery"
build_service "infrastructure/api-gateway" "API Gateway"
build_service "infrastructure/config-server" "Config Server"

# Build business services
echo -e "${YELLOW}Building Business Services...${NC}"
build_service "services/catalog-processor" "Catalog Processor"
build_service "services/nlp-service" "NLP Service"
build_service "services/port-manager" "Port Manager"
build_service "services/url-validation" "URL Validation"

# Build UI
echo -e "${YELLOW}Building UI...${NC}"
build_service "ui" "Frontend UI"

# Build common library
echo -e "${YELLOW}Building Common Library...${NC}"
build_service "common" "Common Library"

echo -e "${GREEN}All services built successfully!${NC}"

# Build Docker images
build_docker_images() {
    if command -v docker >/dev/null 2>&1; then
        print_status "Building Docker images..."
        
        # Enable BuildKit for better performance
        export DOCKER_BUILDKIT=1
        export COMPOSE_DOCKER_CLI_BUILD=1
        
        # Use environment variables for Docker registry and tag
        DOCKER_REGISTRY=${DOCKER_REGISTRY:-spacedataarchive}
        DOCKER_TAG=${DOCKER_TAG:-latest}
        
        # Build all images using docker-compose
        docker-compose build
        
        print_info "Docker images built successfully"
    else
        print_warning "Docker not installed - skipping Docker image builds"
    fi
}

# Run linting checks
run_linting() {
    print_status "Running linting checks..."
    
    # Java linting
    print_info "Running Java checkstyle..."
    mvn checkstyle:check || print_warning "Checkstyle issues found"
    
    # TypeScript linting
    if [ -d "ui" ]; then
        print_info "Running TypeScript linting..."
        cd ui
        npm run lint || print_warning "TypeScript lint issues found"
        cd ..
    fi
}

# Start services after build
start_services() {
    print_status "Starting services..."
    
    if [ -f "./restart-services.sh" ]; then
        print_info "Using restart-services.sh for enhanced service management..."
        chmod +x ./restart-services.sh
        ./restart-services.sh
    else
        print_info "Using run-system.sh for basic service startup..."
        chmod +x ./run-system.sh
        ./run-system.sh
    fi
}

# Main build process
main() {
    print_status "Starting build process..."
    
    # Clean up running processes
    cleanup_processes
    
    # Setup environment
    load_env_files
    
    # Check requirements
    check_requirements
    
    # Build steps
    build_docker_images
    
    # Run linting if not explicitly skipped
    if [ "${SKIP_LINT:-false}" != "true" ]; then
        run_linting
    fi
    
    # Start services as post-build step
    start_services
    
    print_status "Build process completed successfully!"
}

# Run main if script is executed directly
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
fi 