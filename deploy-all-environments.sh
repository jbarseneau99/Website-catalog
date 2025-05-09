#!/bin/bash

# Mach33 All-Environment Deployment Script
# This script builds and deploys the system for all environments:
# 1. Local environment (JAR files with Atlas Hosted DB)
# 2. Staging environment (Docker with Atlas Hosted DB)
# 3. Production environment (via GitHub with Atlas Hosted DB)

# Color coding for better readability
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Function to display section headers
section() {
  echo -e "${BLUE}=======================================================${NC}"
  echo -e "${BLUE}$1${NC}"
  echo -e "${BLUE}=======================================================${NC}"
}

# Function to check required commands
check_command() {
  if ! command -v $1 &> /dev/null; then
    echo -e "${RED}Error: $1 is not installed. Please install it first.${NC}"
    exit 1
  fi
}

# Check for required commands
check_command git
check_command jq
check_command kubectl
check_command docker

# Display menu
section "Mach33 Deployment System"
echo "Choose environment to build and deploy:"
echo "1) Local Environment (JAR files with Atlas Hosted DB)"
echo "2) Staging Environment (Docker with Atlas Hosted DB)"
echo "3) Production Environment (via GitHub with Atlas Hosted DB)"
echo "4) All Environments"
read -p "Enter your choice [1-4]: " ENVIRONMENT_CHOICE

# MongoDB Atlas Configuration
section "MongoDB Atlas Configuration"
echo -e "${YELLOW}Please provide your MongoDB Atlas connection details:${NC}"

read -p "MongoDB Username: " MONGO_USERNAME
read -p "MongoDB Password: " -s MONGO_PASSWORD
echo ""
read -p "MongoDB Cluster Address (e.g., cluster0.abc123.mongodb.net): " MONGO_CLUSTER
read -p "MongoDB Database Name: " MONGO_DATABASE

# Create the connection string
MONGO_CONNECTION_STRING="mongodb+srv://${MONGO_USERNAME}:${MONGO_PASSWORD}@${MONGO_CLUSTER}/${MONGO_DATABASE}"

# Local Environment Setup
setup_local_env() {
  section "Setting up Local Environment"
  echo "Building JAR files and configuring with Atlas Hosted DB..."
  
  # Create env file for local development
  cat > mach33-env.sh << EOF
#!/bin/bash
# Mach33 Environment Configuration
export SPRING_PROFILES_ACTIVE=local
export MONGODB_URI="${MONGO_CONNECTION_STRING}"
EOF
  chmod +x mach33-env.sh
  
  echo -e "${GREEN}Created mach33-env.sh with MongoDB configuration${NC}"
  
  # Setup tree navigation for local testing
  echo "Setting up tree navigation for local testing..."
  ./test-tree-nav-local.sh

  echo -e "${GREEN}Local environment setup complete!${NC}"
  echo "To run the application locally:"
  echo "1. Source the environment file: source mach33-env.sh"
  echo "2. Run your application JAR files"
}

# Staging Environment Setup
setup_staging_env() {
  section "Setting up Staging Environment"
  echo "Building Docker containers and configuring with Atlas Hosted DB..."
  
  # Create Docker compose file for staging
  cat > docker-compose.staging.yml << EOF
version: '3.8'
services:
  admin-service:
    build:
      context: .
      dockerfile: Dockerfile.staging
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - MONGODB_URI=${MONGO_CONNECTION_STRING}
    ports:
      - "8080:8080"
    volumes:
      - ./tree-nav:/app/public/tree-nav
EOF

  # Create staging Dockerfile if it doesn't exist
  if [ ! -f "Dockerfile.staging" ]; then
    cat > Dockerfile.staging << EOF
FROM openjdk:17-slim
WORKDIR /app
COPY target/*.jar app.jar
COPY tree-nav/ /app/public/tree-nav/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
  fi

  echo -e "${GREEN}Created docker-compose.staging.yml and Dockerfile.staging${NC}"
  echo "Building staging Docker image..."
  docker-compose -f docker-compose.staging.yml build

  echo -e "${GREEN}Staging environment setup complete!${NC}"
  echo "To run the staging environment:"
  echo "docker-compose -f docker-compose.staging.yml up"
}

# Production Environment Setup
setup_production_env() {
  section "Setting up Production Environment"
  echo -e "${YELLOW}IMPORTANT: Production deployments use GitHub Actions workflow${NC}"
  
  # Check if we're in a git repository
  if [ ! -d ".git" ]; then
    echo -e "${RED}Not a git repository. Initialize git first.${NC}"
    read -p "Initialize git repository? (y/n): " INIT_GIT
    if [[ "$INIT_GIT" == "y" ]]; then
      git init
      echo "Git repository initialized"
    else
      echo "Aborting production setup"
      return 1
    fi
  fi

  # Check current branch
  CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
  if [[ "$CURRENT_BRANCH" != "main" && "$CURRENT_BRANCH" != "master" ]]; then
    echo -e "${YELLOW}Warning: You are not on main/master branch (current: $CURRENT_BRANCH)${NC}"
    read -p "Continue anyway? (y/n): " CONTINUE_BRANCH
    if [[ "$CONTINUE_BRANCH" != "y" ]]; then
      echo "Aborting production setup"
      return 1
    fi
  fi

  # Create kubernetes secret yaml (this won't be committed to git)
  echo "Creating Kubernetes secret configuration..."
  cat > mongodb-secret.yaml << EOF
apiVersion: v1
kind: Secret
metadata:
  name: mongodb-credentials
  namespace: mach33
type: Opaque
stringData:
  mongodb-uri: "${MONGO_CONNECTION_STRING}"
EOF

  echo -e "${GREEN}Created mongodb-secret.yaml (do NOT commit this file)${NC}"
  echo "Adding to .gitignore..."
  if [ ! -f ".gitignore" ]; then
    echo "mongodb-secret.yaml" > .gitignore
  else
    if ! grep -q "mongodb-secret.yaml" .gitignore; then
      echo "mongodb-secret.yaml" >> .gitignore
    fi
  fi

  # Create production deployment instructions
  cat > PRODUCTION-DEPLOYMENT.md << EOF
# Production Deployment Instructions

## Prerequisites
- GitHub repository configured
- Kubernetes cluster running
- GitHub Actions secrets configured

## GitHub Actions Secrets Required
- KUBE_CONFIG: Base64-encoded Kubernetes config
- MONGODB_URI: MongoDB Atlas connection string

## Deployment Process
1. Push changes to main/master branch
2. GitHub Actions will automatically deploy the changes
3. If automatic trigger doesn't work, manually trigger the workflow in GitHub Actions UI

## Manual Kubernetes Secret Setup
To set up the MongoDB connection, apply the mongodb-secret.yaml file:
\`\`\`
kubectl apply -f mongodb-secret.yaml
\`\`\`

DO NOT commit the mongodb-secret.yaml file to Git!
EOF

  echo -e "${GREEN}Created PRODUCTION-DEPLOYMENT.md with instructions${NC}"
  
  # Check if workflow files exist and are correct
  if [ ! -f ".github/workflows/tree-nav-deploy.yml" ]; then
    echo -e "${RED}Warning: GitHub workflow file missing!${NC}"
    echo "Please ensure .github/workflows/tree-nav-deploy.yml exists"
  else
    echo -e "${GREEN}GitHub workflow files configured.${NC}"
  fi

  echo "To deploy to production:"
  echo "1. Commit your changes"
  echo "2. Push to GitHub repository"
  echo "3. GitHub Actions will deploy automatically based on file changes"
  echo "4. Or manually trigger the workflow from GitHub Actions UI"
}

# Build all environments based on choice
case $ENVIRONMENT_CHOICE in
  1)
    setup_local_env
    ;;
  2)
    setup_staging_env
    ;;
  3)
    setup_production_env
    ;;
  4)
    setup_local_env
    setup_staging_env
    setup_production_env
    ;;
  *)
    echo -e "${RED}Invalid choice. Exiting.${NC}"
    exit 1
    ;;
esac

section "Deployment Summary"
echo -e "${GREEN}All requested environments have been configured!${NC}"
echo ""
echo "Environment details:"

if [[ "$ENVIRONMENT_CHOICE" == "1" || "$ENVIRONMENT_CHOICE" == "4" ]]; then
  echo -e "${BLUE}Local Environment:${NC} JAR files using Atlas Hosted DB"
  echo "  - Configuration file: mach33-env.sh"
  echo "  - Tree navigation tested with local HTTP server"
fi

if [[ "$ENVIRONMENT_CHOICE" == "2" || "$ENVIRONMENT_CHOICE" == "4" ]]; then
  echo -e "${BLUE}Staging Environment:${NC} Docker using Atlas Hosted DB"
  echo "  - Configuration file: docker-compose.staging.yml"
  echo "  - Run with: docker-compose -f docker-compose.staging.yml up"
fi

if [[ "$ENVIRONMENT_CHOICE" == "3" || "$ENVIRONMENT_CHOICE" == "4" ]]; then
  echo -e "${BLUE}Production Environment:${NC} GitHub using Atlas Hosted DB"
  echo "  - Deployed via GitHub Actions workflow"
  echo "  - Instruction file: PRODUCTION-DEPLOYMENT.md"
  echo "  - Remember to apply secret: kubectl apply -f mongodb-secret.yaml"
fi 