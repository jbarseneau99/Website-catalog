# Mach33 Admin UI Tree Navigation

This README explains the enhanced three-tier tree navigation for the Mach33 Admin UI.

## Files

- `adminHierarchy.json` - Defines the menu structure
- `tree-nav.css` - CSS styles for the navigation
- `tree-navigation.js` - JavaScript for navigation interactions
- `tree-nav.html` - HTML structure for the navigation (deployed via ConfigMap)
- `enhance-tree-nav.sh` - Script to deploy the enhanced three-tier navigation
- `fix-admin-ui-path.sh` - Script to fix path issues with the navigation
- `fix-path-issue.sh` - Script to fix HTML path issues for tree navigation assets

## Navigation Structure

The tree navigation now supports a three-tier structure:
1. **Top-level menu items** - Main categories (Dashboard, Agent Ecosystem, etc.)
2. **Submenu items** - Second-level categories under each main category
3. **Third-level items** - Specific pages or features under each subcategory

## Deployment

### Production Deployment (Recommended)

All production deployments should go through GitHub Actions:

1. Make changes to the navigation files:
   - Edit `tree-nav.css` for styling changes
   - Edit `tree-navigation.js` for behavior changes
   - Edit `enhance-tree-nav.sh` for HTML structure changes

2. Commit and push changes to GitHub:
   ```bash
   git add tree-nav.css tree-navigation.js enhance-tree-nav.sh
   git commit -m "Update tree navigation"
   git push origin main
   ```

3. GitHub Actions will automatically deploy the changes to production when changes are pushed to these files.

### Staging/Local Deployment

For testing in local or staging environments, use the following scripts:

```bash
# Deploy the navigation
./enhance-tree-nav.sh

# Fix HTML paths (if needed)
./fix-path-issue.sh
```

## Technical Implementation

### ConfigMap Approach

The navigation is deployed as a ConfigMap in Kubernetes:

1. ConfigMap `admin-ui-tree-nav` contains:
   - `tree-nav.html` - The HTML structure for the navigation
   - `tree-nav.css` - CSS styles for the navigation
   - `tree-navigation.js` - JavaScript for interactivity

2. The files in the ConfigMap are mounted as volumes in the admin-service pod at:
   - `/app/public/tree-nav/`

3. Nginx is configured to serve these files at:
   - `/admin/tree-nav/`

4. ConfigMap `admin-service-html-fixed` contains:
   - `fixed-index.html` - HTML that loads the tree navigation with correct paths

### Path Configuration

Correct path structure is crucial:

1. HTML references in the admin UI:
   - CSS path: `/admin/tree-nav/tree-nav.css`  
   - JS path: `/admin/tree-nav/tree-navigation.js`
   - HTML fetch: `/admin/tree-nav/tree-nav.html`

2. Nginx location block:
   ```
   location ~ ^/admin/tree-nav/(.*)$ {
       alias /app/public/tree-nav/$1;
   }
   ```

### CSS Details

- Uses nested CSS selectors for three-tier depth
- Implements collapsible sections with CSS transitions
- Provides visual cues for active items
- Adds Mach33 theme colors and styling

### JavaScript Details

- Handles opening/closing of menu sections
- Maintains state in URL hash for navigation
- Adds production environment badge
- Loads content based on selected menu item

## Troubleshooting

If the navigation is not appearing or is broken:

1. Check the ConfigMap exists:
   ```bash
   kubectl get configmap admin-ui-tree-nav -n mach33
   ```

2. Check the admin-service pod is running:
   ```bash
   kubectl get pods -n mach33 -l app=admin-service
   ```

3. Check Nginx configuration:
   ```bash
   kubectl exec -it $(kubectl get pods -n mach33 -l app=admin-service -o jsonpath='{.items[0].metadata.name}') -n mach33 -- cat /etc/nginx/conf.d/default.conf
   ```

4. Check file paths:
   ```bash
   kubectl exec -it $(kubectl get pods -n mach33 -l app=admin-service -o jsonpath='{.items[0].metadata.name}') -n mach33 -- ls -la /app/public/tree-nav/
   ```

5. Check HTML file for correct paths:
   ```bash
   kubectl exec -it $(kubectl get pods -n mach33 -l app=admin-service -o jsonpath='{.items[0].metadata.name}') -n mach33 -- cat /usr/share/nginx/html/index.html | grep -A 2 -B 2 tree-nav
   ```

6. Test URL endpoints:
   ```bash
   # Check if CSS is accessible
   curl -sI http://35.226.118.214/admin/tree-nav/tree-nav.css | head -2
   
   # Check if JS is accessible
   curl -sI http://35.226.118.214/admin/tree-nav/tree-navigation.js | head -2
   
   # Check if HTML is accessible
   curl -sI http://35.226.118.214/admin/tree-nav/tree-nav.html | head -2
   ```

7. If path issues are detected, run the fix-path-issue.sh script:
   ```bash
   ./fix-path-issue.sh
   ```

8. Inspect browser console for JavaScript errors

## Common Path Issues

1. **Missing leading slashes**: URLs should be `/admin/tree-nav/...` not `admin/tree-nav/...`
2. **Double slashes**: Avoid `//admin/tree-nav/...`
3. **Incorrect paths in HTML**: The admin UI HTML must reference the correct paths
4. **Nginx configuration**: Location block should properly map `/admin/tree-nav/` to `/app/public/tree-nav/`

## Further Enhancements

Future improvements could include:
- Dynamic loading of menu items from a database or API
- User-specific menu customization
- Permission-based menu visibility
- Collapsible sidebar for mobile responsiveness 