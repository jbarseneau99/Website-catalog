#!/bin/bash

# Exit on any error
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Setup environment
setup_environment() {
    # Add Homebrew to PATH
    if [ -f "/opt/homebrew/bin/brew" ]; then
        eval "$(/opt/homebrew/bin/brew shellenv)"
    fi

    # Set Java environment
    if [ -x "/usr/libexec/java_home" ]; then
        export JAVA_HOME=$(/usr/libexec/java_home -v 17)
        export PATH=$JAVA_HOME/bin:$PATH
    fi
}

# Run environment setup
setup_environment

# Print functions
print_status() { echo -e "${GREEN}[STATUS] $1${NC}"; }
print_warning() { echo -e "${YELLOW}[WARNING] $1${NC}"; }
print_error() { echo -e "${RED}[ERROR] $1${NC}"; }
print_info() { echo -e "${BLUE}[INFO] $1${NC}"; }

# Environment variables
EUREKA_PORT=8761
PORT_MANAGER_PORT=8090
UI_PORT=3000
DYNAMIC_PORT_RANGE_START=8081
DYNAMIC_PORT_RANGE_END=8102  # Updated to include all required ports
UI_URL="http://localhost:${UI_PORT}"
MONGODB_URI="mongodb+srv://jbarseneau:Cheech99@cluster0.rd3fpku.mongodb.net/website-catalog"

# Check MongoDB connection
check_mongodb_connection() {
    print_info "Checking MongoDB Atlas connection..."
    
    # Try to connect using mongosh or mongo
    if command -v mongosh >/dev/null 2>&1; then
        if mongosh "$MONGODB_URI" --eval "db.runCommand({ping: 1})" >/dev/null 2>&1; then
            print_status "✓ MongoDB Atlas connection successful"
            return 0
        fi
    fi
    
    print_error "× Failed to connect to MongoDB Atlas"
    print_info "Please check:"
    print_info "1. Your network connection"
    print_info "2. MongoDB Atlas is running"
    print_info "3. Your IP is whitelisted in MongoDB Atlas"
    print_info "4. Your credentials are correct"
    return 1
}

# Check port availability
check_port_availability() {
    local port=$1
    print_info "Checking availability of port $port..."
    
    if lsof -i ":$port" >/dev/null 2>&1; then
        print_error "× Port $port is already in use"
        return 1
    fi
    
    if [ $port -lt $DYNAMIC_PORT_RANGE_START ] || [ $port -gt $DYNAMIC_PORT_RANGE_END ]; then
        if [ $port != $EUREKA_PORT ] && [ $port != $PORT_MANAGER_PORT ] && [ $port != $UI_PORT ]; then
            print_error "× Port $port is outside allowed range ($DYNAMIC_PORT_RANGE_START-$DYNAMIC_PORT_RANGE_END)"
            return 1
        fi
    fi
    
    print_status "✓ Port $port is available"
    return 0
}

# Wait for service to be ready
wait_for_service() {
    local url=$1
    local timeout=$2
    local count=0
    
    print_info "Waiting for service at $url..."
    while [ $count -lt $timeout ]; do
        if curl -s "$url" >/dev/null; then
            print_status "✓ Service is ready"
            return 0
        fi
        ((count++))
        sleep 1
    done
    
    print_error "× Service failed to start within $timeout seconds"
    return 1
}

# Enhanced service startup
start_service() {
    local service_name=$1
    local jar_path=$2
    local port=$3
    local max_attempts=3
    local attempt=1
    
    if ! check_port_availability $port; then
        return 1
    fi
    
    while [ $attempt -le $max_attempts ]; do
        print_info "Starting $service_name (attempt $attempt)..."
        
        java -jar $jar_path \
            --spring.profiles.active=${ENVIRONMENT} \
            --server.port=$port \
            --eureka.client.serviceUrl.defaultZone=http://localhost:$EUREKA_PORT/eureka/ \
            --spring.data.mongodb.uri="${MONGODB_URI}" \
            --port-manager.url="http://localhost:${PORT_MANAGER_PORT}" \
            --management.endpoints.web.cors.allowed-origins="${UI_URL}" \
            --management.endpoints.web.cors.allowed-methods="GET,POST,PUT,DELETE,OPTIONS" \
            --management.endpoints.web.cors.allowed-headers="*" \
            --management.endpoints.web.cors.allow-credentials=true \
            --logging.level.org.springframework.web.cors=DEBUG \
            > logs/${service_name}-${ENVIRONMENT}.log 2>&1 &
        
        # Wait for service to be healthy
        local health_attempts=30
        local health_attempt=1
        while [ $health_attempt -le $health_attempts ]; do
            if curl -s "http://localhost:$port/actuator/health" | grep -q "UP"; then
                print_status "$service_name is healthy on port $port!"
                return 0
            fi
            sleep 2
            ((health_attempt++))
        done
        
        print_warning "$service_name failed to start on attempt $attempt"
        kill $(lsof -ti:$port) 2>/dev/null || true
        sleep 5
        ((attempt++))
    done
    
    print_error "$service_name failed to start after $max_attempts attempts"
    return 1
}

# Start services in process mode
start_services_process() {
    print_status "Starting services in process mode..."
    
    # Create logs directory
    mkdir -p logs
    
    # Start core services first
    print_info "Starting core services..."
    
    # Start Eureka Server
    if ! start_service "eureka-server" "service-discovery/target/service-discovery-*.jar" $EUREKA_PORT; then
        print_error "Failed to start Eureka Server. Exiting..."
        exit 1
    fi
    
    # Wait for Eureka to be ready
    print_info "Waiting for Eureka Server to be ready..."
    wait_for_service "http://localhost:$EUREKA_PORT/eureka/apps" 30
    
    # Start Port Manager
    if ! start_service "port-manager" "services/port-manager/target/port-manager-*.jar" $PORT_MANAGER_PORT; then
        print_error "Failed to start Port Manager. Exiting..."
        cleanup
        exit 1
    fi
    
    # Wait for Port Manager to be ready
    print_info "Waiting for Port Manager to be ready..."
    wait_for_service "http://localhost:$PORT_MANAGER_PORT/actuator/health" 30
    
    # Start other services with retries
    local services=("catalog-processor" "nlp-service" "url-validation")
    local ports=(8100 8101 8102)
    for i in "${!services[@]}"; do
        local service="${services[$i]}"
        local port="${ports[$i]}"
        local max_retries=3
        local retry=0
        while [ $retry -lt $max_retries ]; do
            print_info "Starting $service (attempt $((retry+1))/$max_retries)..."
            if start_service "$service" "services/$service/target/$service-*.jar" "$port"; then
                break
            fi
            ((retry++))
            if [ $retry -eq $max_retries ]; then
                print_error "Failed to start $service after $max_retries attempts"
                cleanup
                exit 1
            fi
            sleep 5
        done
    done
    
    # Start UI last
    print_info "Starting UI..."
    cd ui
    if ! npm start > ../logs/ui-${ENVIRONMENT}.log 2>&1 & then
        print_error "Failed to start UI"
        cleanup
        exit 1
    fi
    cd ..
    
    print_status "All services started successfully!"
}

# Build all services
build_services() {
    print_status "Building all services..."
    
    # Build Java services
    if [ -f "pom.xml" ]; then
        print_info "Building Java services..."
        # Add retry mechanism for Maven build
        max_attempts=3
        attempt=1
        while [ $attempt -le $max_attempts ]; do
            if ./mvnw clean package -DskipTests; then
                print_status "Maven build successful"
                break
            else
                print_warning "Maven build attempt $attempt failed"
                if [ $attempt -eq $max_attempts ]; then
                    print_error "Maven build failed after $max_attempts attempts"
                    exit 1
                fi
                print_info "Retrying Maven build..."
                sleep 5
            fi
            ((attempt++))
        done
    fi
    
    # Build UI
    if [ -d "ui" ]; then
        print_info "Building UI..."
        cd ui
        # Add retry mechanism for npm install
        max_attempts=3
        attempt=1
        while [ $attempt -le $max_attempts ]; do
            if npm install; then
                print_status "NPM install successful"
                break
            else
                print_warning "NPM install attempt $attempt failed"
                if [ $attempt -eq $max_attempts ]; then
                    print_error "NPM install failed after $max_attempts attempts"
                    exit 1
                fi
                print_info "Retrying NPM install..."
                sleep 5
            fi
            ((attempt++))
        done
        
        if npm run build; then
            print_status "UI build successful"
        else
            print_error "UI build failed"
            exit 1
        fi
        cd ..
    fi
}

# Verify build artifacts
verify_builds() {
    print_status "Verifying build artifacts..."
    
    # Check core services
    local core_services=("service-discovery" "services/port-manager")
    for service in "${core_services[@]}"; do
        if ! ls "${service}/target"/*.jar >/dev/null 2>&1; then
            print_error "Missing JAR for ${service}"
            return 1
        fi
    done
    
    # Automatically detect and check all service directories
    for service_dir in services/*/; do
        if [ -d "$service_dir" ] && [ "$service_dir" != "services/port-manager/" ]; then
            if ! ls "${service_dir}target"/*.jar >/dev/null 2>&1; then
                print_error "Missing JAR for ${service_dir}"
                return 1
            fi
        fi
    done
    
    # Check UI build
    if [ -d "ui" ] && [ ! -d "ui/build" ]; then
        print_error "Missing UI build directory"
        return 1
    fi
    
    print_status "All build artifacts verified successfully"
    return 0
}

# Build Docker images
build_docker_images() {
    print_status "Building Docker images..."
    
    # Build core services
    docker build -t website-catalog/eureka-server:${ENVIRONMENT} ./service-discovery
    docker build -t website-catalog/port-manager:${ENVIRONMENT} ./services/port-manager
    
    # Build application services
    docker build -t website-catalog/url-validation:${ENVIRONMENT} ./services/url-validation
    docker build -t website-catalog/nlp-service:${ENVIRONMENT} ./services/nlp-service
    docker build -t website-catalog/catalog-processor:${ENVIRONMENT} ./services/catalog-processor
    
    # Build UI
    docker build -t website-catalog/ui:${ENVIRONMENT} ./ui
}

# Cleanup functions
cleanup_process() {
    print_status "Cleaning up process-based deployment..."
    
    # Stop Java processes for this environment
    print_info "Stopping Java processes..."
    ps aux | grep "spring.profiles.active=${ENVIRONMENT}" | grep -v "redhat.java" | grep -v grep | awk '{print $2}' | while read -r pid; do
        if [ -n "$pid" ]; then
            print_info "Stopping Java process $pid"
            kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null || true
            sleep 1
        fi
    done
    
    # Stop Node.js processes for this environment
    print_info "Stopping Node.js processes..."
    ps aux | grep "REACT_APP_ENV=${ENVIRONMENT}" | grep -v grep | awk '{print $2}' | while read -r pid; do
        if [ -n "$pid" ]; then
            print_info "Stopping Node.js process $pid"
            kill -15 $pid 2>/dev/null || kill -9 $pid 2>/dev/null || true
            sleep 1
        fi
    done
    
    # Clean up ports more thoroughly
    print_info "Cleaning up ports..."
    # Core service ports
    local core_ports=($EUREKA_PORT $PORT_MANAGER_PORT $UI_PORT)
    # Dynamic port range
    for ((port=DYNAMIC_PORT_RANGE_START; port<=DYNAMIC_PORT_RANGE_END; port++)); do
        core_ports+=($port)
    done
    
    for port in "${core_ports[@]}"; do
        if lsof -ti:$port >/dev/null 2>&1; then
            print_info "Stopping process on port $port"
            lsof -ti:$port | xargs kill -15 2>/dev/null || lsof -ti:$port | xargs kill -9 2>/dev/null || true
            sleep 1
        fi
    done
    
    # Double check no Java processes are left
    if pgrep -f "java.*${ENVIRONMENT}" >/dev/null; then
        print_warning "Found leftover Java processes, forcing cleanup..."
        pkill -9 -f "java.*${ENVIRONMENT}" || true
    fi
    
    # Double check no Node processes are left
    if pgrep -f "node.*${ENVIRONMENT}" >/dev/null; then
        print_warning "Found leftover Node processes, forcing cleanup..."
        pkill -9 -f "node.*${ENVIRONMENT}" || true
    fi
    
    # Verify all ports are free
    local busy_ports=()
    for port in "${core_ports[@]}"; do
        if lsof -ti:$port >/dev/null 2>&1; then
            busy_ports+=($port)
        fi
    done
    
    if [ ${#busy_ports[@]} -gt 0 ]; then
        print_error "Failed to free ports: ${busy_ports[*]}"
        return 1
    fi
    
    print_status "Process cleanup completed"
    return 0
}

cleanup_docker() {
    print_status "Cleaning up Docker-based deployment..."
    
    if command -v docker >/dev/null 2>&1; then
        print_info "Stopping Docker containers..."
        docker-compose -f docker-compose.${ENVIRONMENT}.yml down --volumes --remove-orphans 2>/dev/null || true
        print_info "Cleaning up Docker resources..."
        docker system prune -f --filter "label=environment=${ENVIRONMENT}" 2>/dev/null || true
    else
        print_warning "Docker not installed, skipping Docker cleanup"
    fi
}

cleanup_kubernetes() {
    print_status "Cleaning up Kubernetes-based deployment..."
    
    if command -v kubectl >/dev/null 2>&1; then
        print_info "Cleaning up Kubernetes resources..."
        kubectl delete namespace website-catalog-${ENVIRONMENT} 2>/dev/null || true
        
        while kubectl get namespace website-catalog-${ENVIRONMENT} >/dev/null 2>&1; do
            print_info "Waiting for namespace deletion..."
            sleep 2
        done
    else
        print_warning "kubectl not installed, skipping Kubernetes cleanup"
    fi
}

cleanup_logs() {
    print_status "Cleaning log files..."
    mkdir -p logs
    rm -f logs/*-${ENVIRONMENT}.log 2>/dev/null || true
    print_info "Log files cleaned"
}

# Main cleanup function
cleanup() {
    print_status "Starting cleanup for ${ENVIRONMENT} environment..."
    
    case $DEPLOYMENT_METHOD in
        "process")
            cleanup_process
            ;;
        "docker")
            cleanup_docker
            ;;
        "kubernetes")
            cleanup_kubernetes
            ;;
    esac
    
    cleanup_logs
    print_status "Cleanup completed for ${ENVIRONMENT} environment"
}

# Check and install dependencies
check_dependencies() {
    print_status "Checking system requirements..."
    local requirements_met=true

    # Check Java
    print_info "Checking Java..."
    if ! command -v java >/dev/null 2>&1; then
        print_error "Java is not installed"
        print_info "Please install Java 17 using: brew install openjdk@17"
        requirements_met=false
    else
        java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F. '{print $1}')
        if [ "$java_version" = "17" ]; then
            print_status "✓ Java 17 is installed"
        else
            print_error "× Java 17 is required, but version $java_version was found"
            print_info "Please install Java 17 using: brew install openjdk@17"
            requirements_met=false
        fi
    fi

    # Check Node.js
    print_info "Checking Node.js..."
    if ! command -v node >/dev/null 2>&1; then
        print_error "× Node.js is not installed"
        print_info "Please install Node.js using: brew install node"
        requirements_met=false
    else
        print_status "✓ Node.js is installed"
    fi

    # Check npm
    print_info "Checking npm..."
    if ! command -v npm >/dev/null 2>&1; then
        print_error "× npm is not installed"
        print_info "npm should be installed with Node.js"
        requirements_met=false
    else
        print_status "✓ npm is installed"
    fi

    # Check Maven wrapper
    print_info "Checking Maven wrapper..."
    if [ ! -f "../mvnw" ]; then
        print_error "× Maven wrapper is not installed"
        print_info "Please run: mvn -N io.takari:maven:wrapper"
        requirements_met=false
    else
        print_status "✓ Maven wrapper is installed"
    fi

    # Check UI dependencies
    print_info "Checking UI dependencies..."
    if [ -d "ui" ] && [ ! -d "ui/node_modules" ]; then
        print_error "× UI dependencies are not installed"
        print_info "Please run: cd ui && npm install"
        requirements_met=false
    else
        print_status "✓ UI dependencies are installed"
    fi

    if [ "$requirements_met" = false ]; then
        print_error "Some requirements are not met. Please install the missing dependencies and try again."
        exit 1
    fi

    print_status "All requirements are met! ✓"
}

# Help message
show_help() {
    echo "Usage: $0 [-e <environment>] [-c] [-h]"
    echo
    echo "Start microservices in the specified environment"
    echo
    echo "Options:"
    echo "  -e    Environment to use (dev|staging|prod) [default: dev]"
    echo "  -c    Cleanup only (don't start services)"
    echo "  -h    Show this help message"
    echo
    echo "Examples:"
    echo "  $0              # Start in development environment"
    echo "  $0 -e dev      # Start in development environment"
    echo "  $0 -e staging  # Start in staging environment"
    echo "  $0 -e prod     # Start in production environment"
    echo "  $0 -c          # Only cleanup development environment"
    echo "  $0 -e prod -c  # Only cleanup production environment"
}

# Parse command line options
ENVIRONMENT="dev"
CLEANUP_ONLY=false
while getopts "e:ch" opt; do
    case $opt in
        e)
            case $OPTARG in
                dev|development)
                    ENVIRONMENT="development"
                    DEPLOYMENT_METHOD="process"
                    ;;
                staging)
                    ENVIRONMENT="staging"
                    DEPLOYMENT_METHOD="docker"
                    ;;
                prod|production)
                    ENVIRONMENT="production"
                    DEPLOYMENT_METHOD="kubernetes"
                    ;;
                *)
                    print_error "Invalid environment: $OPTARG"
                    show_help
                    exit 1
                    ;;
            esac
            ;;
        c)
            CLEANUP_ONLY=true
            ;;
        h)
            show_help
            exit 0
            ;;
        \?)
            print_error "Invalid option: -$OPTARG"
            show_help
            exit 1
            ;;
    esac
done

# Main execution
main() {
    print_status "Managing services in ${ENVIRONMENT} environment..."
    
    # Always run cleanup first
    cleanup
    
    if [ "$CLEANUP_ONLY" = true ]; then
        print_info "Cleanup only mode - not starting services"
        exit 0
    fi
    
    # Check dependencies and MongoDB connection
    check_dependencies
    if ! check_mongodb_connection; then
        exit 1
    fi
    
    print_status "Starting services..."
    
    # Ensure environment variables are set
    export SPRING_PROFILES_ACTIVE=${ENVIRONMENT}
    export MONGODB_URI
    export PORT_MANAGER_URL="http://localhost:${PORT_MANAGER_PORT}"
    
    # Create logs directory
    mkdir -p logs
    
    # Start services based on deployment method
    case $DEPLOYMENT_METHOD in
        "process")
            start_services_process
            ;;
        "docker")
            print_error "Docker deployment not yet implemented"
            exit 1
            ;;
        "kubernetes")
            print_error "Kubernetes deployment not yet implemented"
            exit 1
            ;;
        *)
            print_error "Unknown deployment method: $DEPLOYMENT_METHOD"
            exit 1
            ;;
    esac
    
    print_status "All services started successfully in ${ENVIRONMENT} environment!"
    
    # Show appropriate endpoints based on deployment method
    case $DEPLOYMENT_METHOD in
        "process"|"docker")
            print_info "Endpoints:"
            print_info "Eureka Dashboard: http://localhost:$EUREKA_PORT"
            print_info "API Gateway: http://localhost:8080"
            print_info "Port Manager: http://localhost:$PORT_MANAGER_PORT"
            print_info "UI: http://localhost:$UI_PORT"
            ;;
        "kubernetes")
            print_info "Kubernetes endpoints:"
            kubectl get ingress -n website-catalog-${ENVIRONMENT}
            ;;
    esac
}

# Execute main function
main "$@" 