#!/bin/bash

# Script to fix path issues between the admin UI HTML and the tree navigation

NAMESPACE="mach33"

echo "Fixing path issues between HTML and tree navigation"

# Create a temporary directory for the files
TMP_DIR=$(mktemp -d)

# Get the current HTML content
echo "Getting current HTML content..."
kubectl exec -it $(kubectl get pods -n $NAMESPACE -l app=admin-service -o jsonpath='{.items[0].metadata.name}') -n $NAMESPACE -- cat /usr/share/nginx/html/index.html > "$TMP_DIR/index.html"

# Modify the HTML to fix path issues
echo "Modifying HTML to fix path issues..."
cat "$TMP_DIR/index.html" | sed 's|tree-nav/tree-nav.css|/admin/tree-nav/tree-nav.css|g' | \
  sed 's|tree-nav/tree-navigation.js|/admin/tree-nav/tree-navigation.js|g' | \
  sed 's|fetch.*(.*tree-nav/tree-nav.html.*)|fetch("/admin/tree-nav/tree-nav.html")|g' > "$TMP_DIR/fixed-index.html"

# Create a ConfigMap with the fixed HTML
echo "Creating ConfigMap with fixed HTML..."
kubectl create configmap admin-service-html-fixed --from-file="$TMP_DIR/fixed-index.html" -n "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

# Create a patch to update the admin-service deployment
cat > "$TMP_DIR/patch.yaml" << EOF
spec:
  template:
    spec:
      containers:
      - name: admin-service
        volumeMounts:
        - name: admin-service-html-fixed
          mountPath: /usr/share/nginx/html/index.html
          subPath: fixed-index.html
      volumes:
      - name: admin-service-html-fixed
        configMap:
          name: admin-service-html-fixed
EOF

# Apply the patch to the admin-service deployment
echo "Patching admin-service deployment..."
kubectl patch deployment admin-service --patch "$(cat $TMP_DIR/patch.yaml)" -n "$NAMESPACE"

# Wait for the rollout to complete
echo "Waiting for rollout to complete..."
kubectl rollout status deployment/admin-service -n "$NAMESPACE"

# Verify the HTML content has been updated
echo "Verifying HTML content has been updated..."
kubectl exec -it $(kubectl get pods -n $NAMESPACE -l app=admin-service -o jsonpath='{.items[0].metadata.name}') -n $NAMESPACE -- cat /usr/share/nginx/html/index.html | grep -A 2 -B 2 tree-nav

# Clean up
echo "Cleaning up temporary directory: $TMP_DIR"
rm -rf "$TMP_DIR"

echo "Path fix completed!"
echo ""
echo "The admin UI should now load the tree navigation correctly."
echo "Access it at http://35.226.118.214/admin/" 