#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

# Auto-detect environment if not specified
if [ -n "$GITHUB_ACTIONS" ]; then
    DEFAULT_ENV="production"
elif [ -n "$DOCKER_DESKTOP" ] || [ -n "$DOCKER_HOST" ] || command -v docker > /dev/null; then
    DEFAULT_ENV="staging"
else
    DEFAULT_ENV="local"
fi

ENV=${1:-$DEFAULT_ENV}
echo "🚀 Building Mach33 Microservices Platform for ${ENV} environment..."

# Always build from the project root
cd "$(dirname "$0")/.."
ROOT_DIR=$(pwd)

# Load environment variables from mach33-env.sh if available
if [ -f "$ROOT_DIR/../mach33-env.sh" ]; then
    echo "🔧 Loading environment from mach33-env.sh..."
    source "$ROOT_DIR/../mach33-env.sh"
fi

# Environment-specific setup
case $ENV in
    "local")
        echo "🏠 Configuring for local development environment (JAR files)..."
        # Set environment variables for local build
        export SPRING_PROFILES_ACTIVE=local
        # Check if MONGODB_URI is set, if not, prompt user
        if [ -z "$MONGODB_URI" ]; then
            echo "❌ MONGODB_URI is not set. Please provide your MongoDB Atlas connection string."
            echo "Example: mongodb+srv://username:password@host/database"
            read -p "MongoDB URI: " MONGODB_URI
            export MONGODB_URI
        fi
        MAVEN_ARGS="package -DskipTests"
        ;;
    "staging")
        echo "🧪 Configuring for staging environment (desktop Docker)..."
        # Set environment variables for staging build
        export SPRING_PROFILES_ACTIVE=staging
        # Check if MONGODB_URI is set, if not, prompt user
        if [ -z "$MONGODB_URI" ]; then
            echo "❌ MONGODB_URI is not set. Please provide your MongoDB Atlas connection string."
            echo "Example: mongodb+srv://username:password@host/database"
            read -p "MongoDB URI for staging: " MONGODB_URI
            export MONGODB_URI
        fi
        MAVEN_ARGS="package"
        
        # Check if Docker is installed
        if ! command -v docker > /dev/null; then
            echo "⚠️  Warning: Docker not found, but required for staging environment."
            echo "    Please install Docker Desktop or set up Docker environment."
            echo "    Continuing with build, but deployment may fail."
        fi
        ;;
    "production")
        echo "🏭 Configuring for production environment (GitHub deployment)..."
        # Make sure MongoDB URI is set for production
        if [ -z "$MONGODB_URI" ]; then
            echo "❌ MONGODB_URI must be set for production builds"
            echo "Example: export MONGODB_URI=mongodb+srv://user:password@host/database"
            
            # In GitHub environment, fail immediately if not set
            if [ -n "$GITHUB_ACTIONS" ]; then
                exit 1
            else
                # Prompt for URI if not in GitHub environment
                read -p "MongoDB URI for production: " MONGODB_URI
                export MONGODB_URI
            fi
        fi
        export SPRING_PROFILES_ACTIVE=prod
        MAVEN_ARGS="package"
        
        # Additional checks for production
        if [ -n "$GITHUB_ACTIONS" ]; then
            echo "✅ Running in GitHub Actions environment"
        else
            echo "⚠️  Warning: Not running in GitHub Actions but building for production."
            echo "    This is fine for testing, but deployment will require GitHub CI/CD."
        fi
        ;;
    *)
        echo "❌ Unknown environment: $ENV"
        echo "   Supported environments: local, staging, production"
        exit 1
        ;;
esac

echo "📦 Building all services for $ENV environment..."
echo "🔧 Using MongoDB: ${MONGODB_URI:0:20}..." # Show only beginning for security

# Check for Maven
if ! command -v mvn > /dev/null; then
    echo "❌ Maven is required but not found. Please install Maven."
    exit 1
fi

# Run Maven build
echo "🔧 Running Maven build: mvn clean $MAVEN_ARGS"
mvn clean $MAVEN_ARGS

# Check build status
if [ $? -eq 0 ]; then
    echo "✅ Build completed successfully!"
    
    # Report built artifacts
    echo "📂 Built JARs:"
    find . -name "*.jar" -not -path "*/target/dependency/*" -not -path "*/\.*" | while read -r jar_file; do
        jar_size=$(du -h "$jar_file" | cut -f1)
        echo "   - $jar_file ($jar_size)"
    done
    
    # Create or update environment config file
    CONFIG_DIR="$ROOT_DIR/.env-config"
    mkdir -p "$CONFIG_DIR"
    echo "# Mach33 Environment Configuration" > "$CONFIG_DIR/$ENV.env"
    echo "SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE" >> "$CONFIG_DIR/$ENV.env"
    echo "MONGODB_URI=$MONGODB_URI" >> "$CONFIG_DIR/$ENV.env"
    echo "BUILD_TIMESTAMP=$(date +%Y%m%d%H%M%S)" >> "$CONFIG_DIR/$ENV.env"
    echo "✅ Environment configuration saved to $CONFIG_DIR/$ENV.env"
    
    # Output next steps based on environment
    case $ENV in
        "local")
            echo "🚀 To run the services, use: ./scripts/run-local.sh"
            ;;
        "staging")
            echo "🚀 To run the services, use: ./scripts/run-staging.sh"
            ;;
        "production")
            echo "🚀 To run the services, use: ./scripts/run-production.sh"
            ;;
    esac
else
    echo "❌ Build failed"
    exit 1
fi

# Environment-specific post-build steps
case $ENV in
    "local")
        # Nothing special needed for local
        ;;
    "staging"|"production")
        # Check if Docker files need to be generated
        echo "🐳 Ensuring Docker files are up to date..."
        # List of all services including llm-connection
        services=(
            "infrastructure/service-discovery:8761"
            "infrastructure/api-gateway:8080"
            "services/port-manager:8081"
            "services/url-validation:8082"
            "services/nlp-service:8083"
            "services/catalog-processor:8084"
            "services/llm-connection:8085"
            "services/google-search:8762"
        )
        
        for service_info in "${services[@]}"; do
            # Split service_info into service path and port
            IFS=':' read -r service port <<< "$service_info"
            
            if [ ! -f "$ROOT_DIR/$service/Dockerfile" ] || [ "$ROOT_DIR/docker/Dockerfile.template" -nt "$ROOT_DIR/$service/Dockerfile" ]; then
                echo "   Updating Dockerfile for $service..."
                mkdir -p "$ROOT_DIR/$service"
                cp "$ROOT_DIR/docker/Dockerfile.template" "$ROOT_DIR/$service/Dockerfile"
                
                # Update port in Dockerfile
                sed -i'' -e "s/EXPOSE 8080/EXPOSE $port/g" "$ROOT_DIR/$service/Dockerfile"
            fi
        done
        
        # Check if llm-connection service is in docker-compose.yml, if not, add it
        if ! grep -q "llm-connection:" "$ROOT_DIR/docker-compose.yml"; then
            echo "⚠️ llm-connection service not found in docker-compose.yml"
            echo "   You may need to update your docker-compose.yml to include this service"
            echo "   Example service definition to add:"
            echo ""
            echo "  llm-connection:"
            echo "    build:"
            echo "      context: ./services/llm-connection"
            echo "      dockerfile: Dockerfile"
            echo "    container_name: mach33-llm-connection"
            echo "    ports:"
            echo "      - \"8085:8085\""
            echo "    environment:"
            echo "      - SPRING_PROFILES_ACTIVE=\${SPRING_PROFILES_ACTIVE:-local}"
            echo "      - MONGODB_URI=\${MONGODB_URI}"
            echo "      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://service-discovery:8761/eureka/"
            echo "    depends_on:"
            echo "      service-discovery:"
            echo "        condition: service_healthy"
            echo "      api-gateway:"
            echo "        condition: service_healthy"
            echo "    networks:"
            echo "      - mach33-network"
        fi
        ;;
esac

echo "🎉 All done! The Mach33 Microservices Platform is ready for $ENV deployment." 