#!/bin/bash

# Exit on any error
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_status() { echo -e "${GREEN}[STATUS] $1${NC}"; }
print_warning() { echo -e "${YELLOW}[WARNING] $1${NC}"; }
print_error() { echo -e "${RED}[ERROR] $1${NC}"; }
print_info() { echo -e "${BLUE}[INFO] $1${NC}"; }

# Help message
show_help() {
    echo "Usage: $0 [-e <environment>] [-h]"
    echo
    echo "Clean up microservices in the specified environment"
    echo
    echo "Options:"
    echo "  -e    Environment to clean (dev|staging|prod) [default: dev]"
    echo "  -h    Show this help message"
    echo
    echo "Examples:"
    echo "  $0              # Clean development environment"
    echo "  $0 -e dev      # Clean development environment"
    echo "  $0 -e staging  # Clean staging environment"
    echo "  $0 -e prod     # Clean production environment"
}

# Parse command line options
ENVIRONMENT="dev"
while getopts "e:h" opt; do
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

# Environment handling
CONFIG_DIR="config/environments"

# Load environment-specific configuration
load_environment_config() {
    local env_file="${CONFIG_DIR}/${ENVIRONMENT}.env"
    if [ -f "$env_file" ]; then
        print_info "Loading configuration for ${ENVIRONMENT} environment"
        set -a
        source "$env_file"
        set +a
    else
        print_warning "No configuration file found for ${ENVIRONMENT} environment"
        # Set default deployment method based on environment
        case $ENVIRONMENT in
            "development")
                DEPLOYMENT_METHOD="process"
                ;;
            "staging")
                DEPLOYMENT_METHOD="docker"
                ;;
            "production")
                DEPLOYMENT_METHOD="kubernetes"
                ;;
            *)
                print_error "Unknown environment: ${ENVIRONMENT}"
                exit 1
                ;;
        esac
    fi
}

# Known service ports (will be overridden by environment config if present)
CORE_PORTS=(
    8761  # Eureka Server
    8090  # Port Manager
    3000  # UI
)

# Function to stop processes on a port
stop_port() {
    local port=$1
    if lsof -ti:$port >/dev/null 2>&1; then
        print_info "Stopping process on port $port in ${ENVIRONMENT} environment"
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
}

# Cleanup for process-based deployment
cleanup_process() {
    print_status "Cleaning up process-based deployment..."
    
    # Stop Java processes for this environment
    print_info "Stopping Java processes..."
    ps aux | grep "spring.profiles.active=${ENVIRONMENT}" | grep -v "redhat.java" | grep -v grep | awk '{print $2}' | while read -r pid; do
        if [ -n "$pid" ]; then
            print_info "Stopping Java process $pid"
            kill -9 $pid 2>/dev/null || true
        fi
    done
    
    # Stop Node.js processes for this environment
    print_info "Stopping Node.js processes..."
    ps aux | grep "REACT_APP_ENV=${ENVIRONMENT}" | grep -v grep | awk '{print $2}' | while read -r pid; do
        if [ -n "$pid" ]; then
            print_info "Stopping Node.js process $pid"
            kill -9 $pid 2>/dev/null || true
        fi
    done
    
    # Clean up ports
    print_info "Cleaning up ports..."
    for port in "${CORE_PORTS[@]}"; do
        stop_port "$port"
    done
}

# Cleanup for Docker-based deployment
cleanup_docker() {
    print_status "Cleaning up Docker-based deployment..."
    
    if command -v docker >/dev/null 2>&1; then
        # Stop and remove containers with matching environment
        print_info "Stopping Docker containers..."
        docker-compose -f docker-compose.${ENVIRONMENT}.yml down --volumes --remove-orphans 2>/dev/null || true
        
        # Clean up unused resources
        print_info "Cleaning up Docker resources..."
        docker system prune -f --filter "label=environment=${ENVIRONMENT}" 2>/dev/null || true
    else
        print_warning "Docker not installed, skipping Docker cleanup"
    fi
}

# Cleanup for Kubernetes-based deployment
cleanup_kubernetes() {
    print_status "Cleaning up Kubernetes-based deployment..."
    
    if command -v kubectl >/dev/null 2>&1; then
        # Delete resources in the environment-specific namespace
        print_info "Cleaning up Kubernetes resources..."
        kubectl delete namespace website-catalog-${ENVIRONMENT} 2>/dev/null || true
        
        # Wait for namespace deletion
        while kubectl get namespace website-catalog-${ENVIRONMENT} >/dev/null 2>&1; do
            print_info "Waiting for namespace deletion..."
            sleep 2
        done
    else
        print_warning "kubectl not installed, skipping Kubernetes cleanup"
    fi
}

# Function to clean log files
clean_logs() {
    print_status "Cleaning log files for ${ENVIRONMENT} environment..."
    if [ -d "logs" ]; then
        # Only remove logs for the specific environment
        rm -f logs/*-${ENVIRONMENT}.log 2>/dev/null || true
        print_info "Log files cleaned for ${ENVIRONMENT} environment"
    fi
}

# Main cleanup function
main() {
    print_status "Starting comprehensive cleanup for ${ENVIRONMENT} environment..."
    
    # Load environment-specific configuration
    load_environment_config
    
    # Perform cleanup based on deployment method
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
        *)
            print_error "Unknown deployment method: $DEPLOYMENT_METHOD"
            exit 1
            ;;
    esac
    
    # Clean logs
    clean_logs
    
    print_status "Cleanup completed successfully for ${ENVIRONMENT} environment!"
    print_info "The system is ready for a fresh start in ${ENVIRONMENT} environment"
}

# Execute main function
main 