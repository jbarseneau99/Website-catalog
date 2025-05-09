# Mach33 Deployment Guide

This document provides instructions for deploying the Mach33 platform in all supported environments.

## Overview

Mach33 supports three deployment environments:

1. **Local Environment**: Using JAR files with Atlas Hosted DB
2. **Staging Environment**: Using Docker with Atlas Hosted DB
3. **Production Environment**: Using GitHub with Atlas Hosted DB (recommended)

## Quick Start

For a guided deployment process, use the all-in-one deployment script:

```bash
./deploy-all-environments.sh
```

This script will guide you through setting up any or all of the three environments.

## MongoDB Atlas Configuration

All environments use MongoDB Atlas as the database. You'll need:

- MongoDB Atlas account
- Database cluster created
- Database user with read/write permissions
- Connection string in the format:
  `mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>`

## Local Environment

For local development and testing:

1. Set up environment variables:
   ```bash
   source mach33-env.sh
   ```

2. Run the Java application:
   ```bash
   java -jar target/admin-service.jar
   ```

3. Test the tree navigation:
   ```bash
   ./test-tree-nav-local.sh
   ```

## Staging Environment

For Docker-based staging deployment:

1. Build and run the Docker container:
   ```bash
   docker-compose -f docker-compose.staging.yml up
   ```

2. Access the application at `http://localhost:8080`

## Production Environment

**IMPORTANT: All production deployments must go through GitHub Actions.**

### Prerequisites

1. GitHub repository configured
2. Kubernetes cluster running
3. GitHub Actions secrets set up:
   - `KUBE_CONFIG`: Base64-encoded Kubernetes config
   - `MONGODB_URI`: MongoDB Atlas connection string

### Deployment Process

1. Make changes to relevant files
2. Commit and push to the `main` branch
3. GitHub Actions workflow will automatically deploy changes
4. If automatic triggers don't work, manually trigger the workflow in GitHub Actions UI

### Manual GitHub Workflow Triggering

1. Go to GitHub repository
2. Click on "Actions" tab
3. Select "Deploy Tree Navigation" workflow
4. Click "Run workflow" button
5. Select branch and click "Run workflow"

### MongoDB Secret Setup

Apply the MongoDB secret to Kubernetes:

```bash
kubectl apply -f mongodb-secret.yaml
```

Do NOT commit this file to Git!

## Troubleshooting

### Local Environment

- Verify MongoDB connection string in `mach33-env.sh`
- Check application logs for connection errors

### Staging Environment

- Verify MongoDB connection in Docker environment variables
- Check Docker logs: `docker-compose -f docker-compose.staging.yml logs`

### Production Environment

- Verify GitHub Actions workflow execution in GitHub
- Check Kubernetes pod logs:
  ```bash
  kubectl get pods -n mach33
  kubectl logs -n mach33 [pod-name]
  ```
- Check configmaps:
  ```bash
  kubectl get configmap -n mach33
  ```
- Verify MongoDB secret:
  ```bash
  kubectl get secret -n mach33
  ``` 