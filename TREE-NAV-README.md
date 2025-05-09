# Mach33 Admin UI Tree Navigation

This README explains how to implement and fix the three-tier tree navigation for the Mach33 Admin UI.

## Files

- `adminHierarchy.json` - Defines the menu structure
- `tree-nav.css` - CSS styles for the navigation
- `tree-navigation.js` - JavaScript for navigation interactions
- `fix-admin-ui-path.sh` - Script to fix path issues with the navigation

## Issues Fixed

1. **Path Issues**: The original deployment had incorrect paths for CSS and JS files. We fixed this by changing:
   - `/tree-nav/tree-nav.css` -> `tree-nav/tree-nav.css` (removed leading slash)
   - `/tree-nav/tree-navigation.js` -> `tree-nav/tree-navigation.js` (removed leading slash)

2. **Namespace Issues**: The original script used the wrong namespace. We updated it to use `mach33` instead of `mach33-system`.

3. **HTML Structure Issues**: We added proper HTML structure with the `tree-navigation-container` div inside the proper navigation menu.

4. **GitHub Actions**: Added a workflow file that will automatically deploy the navigation when key files change.

## Deployment Environments

The solution works in all three required environments:

1. **Local Environment**: Using JAR files with Atlas Hosted DB
   - Run the test-tree-nav-local.sh script for local development and testing
   - The navigation files are accessible via a local HTTP server

2. **Staging Environment**: Using Docker with Atlas Hosted DB
   - For testing in a Docker environment before production deployment
   - Use test-tree-nav-local.sh with the Docker flag: `./test-tree-nav-local.sh docker`

3. **Production Environment**: Using GitHub with Atlas Hosted DB
   - **IMPORTANT**: ALL production deployments MUST go through GitHub Actions
   - Direct kubectl commands or manual scripts SHOULD NOT be used for production
   - The workflow is triggered automatically when navigation files are modified
   - Can be manually triggered via the GitHub Actions interface if needed

## GitHub Workflow

Our GitHub workflow (.github/workflows/tree-nav-deploy.yml) ensures:

1. Consistent deployments with proper validation
2. Automatic triggering when relevant files change
3. Full audit trail of all deployments
4. Verification of successful deployment

To manually trigger a deployment from GitHub:
1. Go to the repository on GitHub
2. Click on "Actions"
3. Select "Deploy Tree Navigation" workflow
4. Click "Run workflow"
5. Select the branch and click "Run workflow"

## Local Testing

To test the navigation locally without Kubernetes:

```bash
./test-tree-nav-local.sh
```

This will start a local web server where you can test the navigation in your browser.

## Troubleshooting

If the navigation doesn't appear:

1. Check browser console for errors
2. Verify the ConfigMaps exist: `kubectl get configmap -n mach33 | grep tree-nav`
3. Check the admin-service pod logs: `kubectl logs -n mach33 deployment/admin-service`
4. Try clearing your browser cache
5. Trigger a new deployment via GitHub Actions 