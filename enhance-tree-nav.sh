#!/bin/bash

# Script to enhance the tree navigation to show full hierarchical structure

NAMESPACE="mach33"

echo "Enhancing tree navigation to show full hierarchical structure"

# Create a temporary directory for the files
TMP_DIR=$(mktemp -d)

# Generate enhanced HTML for the tree navigation
echo "Generating enhanced navigation HTML..."

cat > "$TMP_DIR/tree-nav.html" << 'EOF'
<!-- Three-tier Tree Navigation -->
<ul class="sidebar-menu">
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Dashboard</div>
      <div class="menu-arrow"></div>
    </div>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Agent Ecosystem</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Agent Health Monitor</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Status Dashboard</li>
          <li class="third-level-item">Performance Metrics</li>
          <li class="third-level-item">Communication Patterns</li>
          <li class="third-level-item">Resource Utilization</li>
          <li class="third-level-item">Error Rates</li>
          <li class="third-level-item">Response Time Tracking</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Agent Behavior Control</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Policy Management</li>
          <li class="third-level-item">Decision Controls</li>
          <li class="third-level-item">Self-healing Rules</li>
          <li class="third-level-item">Behavioral Boundaries</li>
          <li class="third-level-item">Context Adaptation Settings</li>
          <li class="third-level-item">Safety Protocols</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Agent Lifecycle Management</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Deployment Controls</li>
          <li class="third-level-item">Version Management</li>
          <li class="third-level-item">Configuration History</li>
          <li class="third-level-item">Capability Registry</li>
          <li class="third-level-item">Retirement Planning</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Agent Collaboration</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Interaction Mapping</li>
          <li class="third-level-item">Dependency Tracking</li>
          <li class="third-level-item">Collective Intelligence Analysis</li>
          <li class="third-level-item">Communication Protocols</li>
          <li class="third-level-item">Task Distribution</li>
          <li class="third-level-item">Conflict Resolution</li>
        </ul>
      </li>
    </ul>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Language Intelligence</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Internal LLM Management</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Model Deployment Status</li>
          <li class="third-level-item">Inference Performance</li>
          <li class="third-level-item">Version Control</li>
          <li class="third-level-item">Parameter Management</li>
          <li class="third-level-item">Quantization Control</li>
          <li class="third-level-item">Weight Updates</li>
          <li class="third-level-item">Distribution Settings</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Dynamic Language Models</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Adaptation Controls</li>
          <li class="third-level-item">Context Management</li>
          <li class="third-level-item">Specialization Settings</li>
          <li class="third-level-item">Domain-Specific Tuning</li>
          <li class="third-level-item">Fine-tuning Operations</li>
          <li class="third-level-item">Contextual Knowledge Integration</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Prompt Engineering Hub</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">Template Management</li>
          <li class="third-level-item">Prompt Testing</li>
          <li class="third-level-item">Version Control</li>
          <li class="third-level-item">Performance Analytics</li>
          <li class="third-level-item">Context Window Optimization</li>
          <li class="third-level-item">Pattern Library</li>
        </ul>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Natural Language Operations</div>
          <div class="submenu-arrow"></div>
        </div>
        <ul class="third-level-menu">
          <li class="third-level-item">NLP Pipeline Monitor</li>
          <li class="third-level-item">Language Processing Metrics</li>
          <li class="third-level-item">Linguistic Quality Analysis</li>
          <li class="third-level-item">Multilingual Support</li>
          <li class="third-level-item">Semantic Accuracy</li>
          <li class="third-level-item">Entity Recognition Performance</li>
          <li class="third-level-item">Intent Classification Health</li>
        </ul>
      </li>
    </ul>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Knowledge Management</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Data Engineering</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Knowledge Graphs</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Enterprise Knowledge Base</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Semantic Search</div>
        </div>
      </li>
    </ul>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">System Performance</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Metrics Dashboard</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Resource Optimization</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Performance Analytics</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">System Health</div>
        </div>
      </li>
    </ul>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Security & Compliance</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Access Control</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Vulnerability Management</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Regulatory Compliance</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Audit Trails</div>
        </div>
      </li>
    </ul>
  </li>
  <li class="menu-item">
    <div class="menu-header">
      <div class="menu-icon"></div>
      <div class="menu-title">Settings</div>
      <div class="menu-arrow"></div>
    </div>
    <ul class="submenu">
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">User Management</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">System Configuration</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Integration Settings</div>
        </div>
      </li>
      <li class="submenu-item">
        <div class="submenu-header">
          <div class="submenu-icon"></div>
          <div class="submenu-title">Appearance</div>
        </div>
      </li>
    </ul>
  </li>
</ul>
EOF

# Keep the same navigation JS and CSS

# Create/update the ConfigMap with the enhanced tree navigation
echo "Updating ConfigMap with enhanced tree navigation"
kubectl create configmap admin-ui-tree-nav \
  --from-file="$TMP_DIR/tree-nav.html" \
  --from-file="tree-nav.css" \
  --from-file="tree-navigation.js" \
  --namespace "$NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart the admin-service to apply changes
echo "Restarting admin-service deployment"
kubectl rollout restart deployment admin-service -n "$NAMESPACE"

# Wait for the admin-service to be ready
echo "Waiting for admin-service to be ready..."
kubectl rollout status deployment admin-service -n "$NAMESPACE"

# Clean up
echo "Cleaning up temporary directory: $TMP_DIR"
rm -rf "$TMP_DIR"

echo "Enhanced tree navigation deployment complete!"
echo ""
echo "The admin UI should now display the full navigation structure."
echo "Access it at http://35.226.118.214/admin/" 