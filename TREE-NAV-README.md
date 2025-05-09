# Mach33 Admin UI Tree Navigation

This README explains the enhanced three-tier tree navigation for the Mach33 Admin UI.

## Files

- `adminHierarchy.json` - Defines the menu structure
- `tree-nav.css` - CSS styles for the navigation
- `tree-navigation.js` - JavaScript for navigation interactions
- `tree-nav.html` - HTML structure for the navigation (deployed via ConfigMap)
- `enhance-tree-nav.sh` - Script to deploy the enhanced three-tier navigation
- `fix-admin-ui-path.sh` - Script to fix path issues with the navigation

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

For testing in local or staging environments, use the `enhance-tree-nav.sh` script:

```bash
./enhance-tree-nav.sh
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

5. Inspect browser console for JavaScript errors

## Further Enhancements

Future improvements could include:
- Dynamic loading of menu items from a database or API
- User-specific menu customization
- Permission-based menu visibility
- Collapsible sidebar for mobile responsiveness 