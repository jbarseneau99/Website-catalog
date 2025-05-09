#!/bin/bash

set -e  # Exit immediately if a command exits with a non-zero status

echo "🚀 Running Mach33 Microservices Platform in production mode..."

# Load environment variables if not already set
if [ -z "$MONGODB_URI" ] || [ -z "$SPRING_PROFILES_ACTIVE" ] || [ -z "$GITHUB_TOKEN" ]; then
  if [ -f "$(dirname "$0")/../../mach33-env.sh" ]; then
    echo "🔧 Loading environment from mach33-env.sh..."
    source "$(dirname "$0")/../../mach33-env.sh"
  else
    echo "ℹ️ No mach33-env.sh file found. You can create one from the template:"
    echo "   cp mach33-env.sh.template mach33-env.sh"
    echo "   Then edit mach33-env.sh with your settings."
    
if [ -z "$MONGODB_URI" ]; then
      echo "❌ Error: MONGODB_URI environment variable not set."
      echo "You must set this variable for production deployment."
        exit 1
    fi
    
    if [ -z "$GITHUB_TOKEN" ] && [ "$1" = "--trigger" ]; then
      echo "❌ Error: GITHUB_TOKEN environment variable not set."
      echo "You must set this variable for triggering GitHub deployment."
      echo "You will be prompted to enter it when needed."
    fi
  fi
fi

# Set active profile to production
export SPRING_PROFILES_ACTIVE="prod"

# Ensure GitHub token is exported to child processes
if [ -n "$GITHUB_TOKEN" ]; then
  export GITHUB_TOKEN
  echo "✅ GitHub token is set"
fi

# Check for deployment target and ensure GCP variables are exported if needed
if [ "$DEPLOYMENT_TARGET" = "gcp" ]; then
  echo "🌩️ Target deployment: Google Cloud Platform"
  
  # Check for required GCP environment variables
  if [ -z "$GCP_PROJECT_ID" ]; then
    echo "⚠️ Warning: GCP_PROJECT_ID is not set in mach33-env.sh"
    echo "   This is required for GCP deployment"
    read -p "Enter your GCP Project ID: " GCP_PROJECT_ID
    export GCP_PROJECT_ID
fi

  # Export GCP variables to child processes
  export GCP_PROJECT_ID
  export GCP_ZONE
  export GCP_CLUSTER
  export DEPLOYMENT_TARGET
else
  echo "☁️ Target deployment: Default GitHub workflow"
  export DEPLOYMENT_TARGET="github-default"
fi

# Check if running in GitHub Actions
if [ -n "$GITHUB_ACTIONS" ]; then
  echo "✅ Running in GitHub Actions environment. Continuing with deployment..."
  
  # If running in GitHub Actions, we just need to trigger the right process
  echo "🔄 GitHub Actions will handle the deployment process."
  exit 0
fi

# Check if we should initiate a GitHub deployment
if [ "$1" = "--trigger" ]; then
  echo "🚀 Triggering GitHub Actions workflow for production deployment..."

  # First check if workflow files exist locally
  WORKFLOW_DIR="$(dirname "$0")/../.github/workflows"
  if [ ! -d "$WORKFLOW_DIR" ] || [ -z "$(ls -A "$WORKFLOW_DIR" 2>/dev/null)" ]; then
    echo "⚠️ Warning: No workflow files found in $WORKFLOW_DIR"
    
    # Check if they exist in the root repo instead
    ROOT_WORKFLOW_DIR="$(dirname "$0")/../../.github/workflows"
    if [ -d "$ROOT_WORKFLOW_DIR" ] && [ -n "$(ls -A "$ROOT_WORKFLOW_DIR" 2>/dev/null)" ]; then
      echo "✅ Found workflow files in repository root: $ROOT_WORKFLOW_DIR"
      ls -la "$ROOT_WORKFLOW_DIR"
    else
      echo "❌ No workflow files found in the repository at all."
      echo "You need to create GitHub workflow files before deployment."
      echo "They should be in either:"
      echo "  - $WORKFLOW_DIR"
      echo "  - $ROOT_WORKFLOW_DIR"
      echo ""
      echo "Do you want to continue anyway? (y/n)"
      read -r response
      if [[ "$response" != "y" ]]; then
        echo "Exiting. Please set up workflow files first."
        exit 1
    fi
    fi
  else
    echo "✅ Found workflow files in: $WORKFLOW_DIR"
    ls -la "$WORKFLOW_DIR"
  fi
  
  # Use the dedicated trigger script
  TRIGGER_SCRIPT="$(dirname "$0")/trigger-github-deployment.sh"
  
  if [ -f "$TRIGGER_SCRIPT" ]; then
    echo "🔄 Using trigger-github-deployment.sh to initiate deployment..."
    
    # Load GitHub token from environment if available
    if [ -n "$GITHUB_TOKEN" ]; then
      echo "✅ Found GitHub token in environment variables."
      export GITHUB_TOKEN
      echo "Token: ${GITHUB_TOKEN:0:4}...${GITHUB_TOKEN: -4}" # Show first and last 4 chars for verification
    else
      echo "ℹ️ No GITHUB_TOKEN found in environment variables."
      echo "   You will be prompted to enter it."
      echo "   You can add it to your mach33-env.sh file for future use."
    fi
    
    # Execute the trigger script (it will handle token prompting)
    "$TRIGGER_SCRIPT"
    TRIGGER_EXIT_CODE=$?
    
    if [ $TRIGGER_EXIT_CODE -ne 0 ]; then
      echo "ℹ️ Trigger script exited with code $TRIGGER_EXIT_CODE"
      echo "Check the error messages above for more information."
      echo ""
      echo "Common troubleshooting steps:"
      echo "1. Ensure workflow files exist and are pushed to GitHub"
      echo "2. Verify your GitHub token has 'repo' and 'workflow' permissions"
      echo "3. Check that the MONGODB_URI secret is set in your GitHub repository settings"
    
      if [ "$DEPLOYMENT_TARGET" = "gcp" ]; then
        echo "4. For GCP deployment, ensure these secrets are set in your GitHub repository:"
        echo "   - GCP_PROJECT_ID: Your Google Cloud project ID"
        echo "   - GCP_SA_KEY: Service account key with proper permissions"
        echo ""
        echo "   You can generate a service account key with these commands:"
        echo "   gcloud iam service-accounts create github-actions"
        echo "   gcloud projects add-iam-policy-binding $GCP_PROJECT_ID --member=\"serviceAccount:github-actions@$GCP_PROJECT_ID.iam.gserviceaccount.com\" --role=\"roles/container.admin\""
        echo "   gcloud projects add-iam-policy-binding $GCP_PROJECT_ID --member=\"serviceAccount:github-actions@$GCP_PROJECT_ID.iam.gserviceaccount.com\" --role=\"roles/storage.admin\""
        echo "   gcloud iam service-accounts keys create key.json --iam-account=github-actions@$GCP_PROJECT_ID.iam.gserviceaccount.com"
      fi
    fi
    
    exit $TRIGGER_EXIT_CODE
  else
    echo "❌ Error: trigger-github-deployment.sh script not found."
    echo "Please ensure it exists at: $TRIGGER_SCRIPT"
    exit 1
  fi
fi

# Show error if we're not in GitHub Actions but trying to run production environment locally
echo "⚠️ Production environment is designed to run on GitHub!"
echo "You have three options:"

echo "1. Trigger the GitHub Actions workflow for production deployment:"
echo "   ./scripts/run-production.sh --trigger"
echo ""

echo "2. Use the staging environment instead for local testing with Docker:"
echo "   ./scripts/run-staging.sh"
echo ""

echo "3. Force local simulation of production (not recommended):"
echo "   ./scripts/run-production.sh --force-local"
echo ""

# If --force-local is specified, fall back to Docker for simulation
if [ "$1" = "--force-local" ]; then
  echo "⚠️ WARNING: Simulating production environment locally. This is not recommended."
  
  # Navigate to the project root
  cd "$(dirname "$0")/.."
  
  # Check if docker-compose is available
  if command -v docker-compose &> /dev/null; then
    echo "🐳 Using Docker to simulate production environment locally..."
    
    # Export variables for Docker Compose
    export MONGODB_URI
    export SPRING_PROFILES_ACTIVE
    
    # Run with docker-compose
    docker-compose up -d
    
    echo "✅ Services started in Docker containers."
    echo "📊 Eureka Dashboard: http://localhost:8761"
    echo "🌐 API Gateway: http://localhost:8080"

    echo ""
    echo "To stop the services, run:"
    echo "docker-compose down"
  else
    echo "❌ Error: Docker Compose not available. Cannot simulate production environment locally."
    exit 1
  fi
else
  # Exit if not forcing local simulation
  exit 1
fi 