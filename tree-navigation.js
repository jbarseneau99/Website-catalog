// Tree Navigation JavaScript

document.addEventListener('DOMContentLoaded', function() {
  // Initialize the navigation
  initializeNavigation();
  
  // Add the production badge if we're in production
  addProductionBadgeIfNeeded();
});

function initializeNavigation() {
  // Set up event listeners for menu items
  setupMenuItemListeners();
  
  // Open the Dashboard by default
  const defaultMenuItem = document.querySelector('.menu-item:first-child');
  if (defaultMenuItem) {
    activateMenuItem(defaultMenuItem);
  }
  
  // Set up URL hash navigation
  setupHashNavigation();
}

function setupMenuItemListeners() {
  // First level menu items
  const menuItems = document.querySelectorAll('.menu-item > .menu-header');
  menuItems.forEach(function(menuHeader) {
    menuHeader.addEventListener('click', function() {
      const menuItem = this.parentElement;
      
      // If already active and has submenu, just toggle
      if (menuItem.classList.contains('active') && menuItem.querySelector('.submenu')) {
        menuItem.classList.toggle('active');
        return;
      }
      
      // Close all open menus at this level
      document.querySelectorAll('.menu-item.active').forEach(function(activeItem) {
        if (activeItem !== menuItem) {
          activeItem.classList.remove('active');
        }
      });
      
      // Activate this menu item
      menuItem.classList.toggle('active');
      
      // Update URL hash
      const menuTitle = menuItem.querySelector('.menu-title').textContent;
      window.location.hash = encodeURIComponent(menuTitle.trim());
    });
  });
  
  // Second level menu items
  const submenuItems = document.querySelectorAll('.submenu-item > .submenu-header');
  submenuItems.forEach(function(submenuHeader) {
    submenuHeader.addEventListener('click', function(e) {
      e.stopPropagation(); // Prevent event from bubbling up
      
      const submenuItem = this.parentElement;
      const parentMenuItem = submenuItem.closest('.menu-item');
      
      // If it has a third level menu
      if (submenuItem.querySelector('.third-level-menu')) {
        // If already active, just toggle
        if (submenuItem.classList.contains('active')) {
          submenuItem.classList.toggle('active');
          return;
        }
        
        // Close other active submenu items
        parentMenuItem.querySelectorAll('.submenu-item.active').forEach(function(activeSubItem) {
          if (activeSubItem !== submenuItem) {
            activeSubItem.classList.remove('active');
          }
        });
        
        // Activate this submenu item
        submenuItem.classList.toggle('active');
        
        // Update URL hash
        const menuTitle = parentMenuItem.querySelector('.menu-title').textContent;
        const submenuTitle = submenuItem.querySelector('.submenu-title').textContent;
        window.location.hash = encodeURIComponent(menuTitle.trim() + '/' + submenuTitle.trim());
      }
    });
  });
  
  // Third level menu items
  const thirdLevelItems = document.querySelectorAll('.third-level-item');
  thirdLevelItems.forEach(function(thirdLevelItem) {
    thirdLevelItem.addEventListener('click', function(e) {
      e.stopPropagation(); // Prevent event from bubbling up
      
      // Deactivate all third level items
      document.querySelectorAll('.third-level-item.active').forEach(function(activeItem) {
        activeItem.classList.remove('active');
      });
      
      // Activate this item
      this.classList.add('active');
      
      // Update URL hash
      const menuTitle = this.closest('.menu-item').querySelector('.menu-title').textContent;
      const submenuTitle = this.closest('.submenu-item').querySelector('.submenu-title').textContent;
      const thirdLevelTitle = this.textContent;
      window.location.hash = encodeURIComponent(
        menuTitle.trim() + '/' + submenuTitle.trim() + '/' + thirdLevelTitle.trim()
      );
      
      // Here you would typically load the content for this menu item
      loadContentForMenuItem(menuTitle, submenuTitle, thirdLevelTitle);
    });
  });
}

function activateMenuItem(menuItem) {
  menuItem.classList.add('active');
}

function setupHashNavigation() {
  // Check if there's a hash in the URL and navigate to that section
  if (window.location.hash) {
    const hash = decodeURIComponent(window.location.hash.substring(1));
    const parts = hash.split('/');
    
    if (parts.length >= 1) {
      // Find and activate the main menu item
      const menuItems = document.querySelectorAll('.menu-item');
      menuItems.forEach(function(item) {
        const title = item.querySelector('.menu-title').textContent.trim();
        if (title === parts[0]) {
          item.classList.add('active');
          
          if (parts.length >= 2) {
            // Find and activate the submenu item
            const submenuItems = item.querySelectorAll('.submenu-item');
            submenuItems.forEach(function(subItem) {
              const subTitle = subItem.querySelector('.submenu-title').textContent.trim();
              if (subTitle === parts[1]) {
                subItem.classList.add('active');
                
                if (parts.length >= 3) {
                  // Find and activate the third level item
                  const thirdLevelItems = subItem.querySelectorAll('.third-level-item');
                  thirdLevelItems.forEach(function(thirdItem) {
                    if (thirdItem.textContent.trim() === parts[2]) {
                      thirdItem.classList.add('active');
                      
                      // Load content for this menu item
                      loadContentForMenuItem(parts[0], parts[1], parts[2]);
                    }
                  });
                }
              }
            });
          }
        }
      });
    }
  }
}

function loadContentForMenuItem(menuTitle, submenuTitle, thirdLevelTitle) {
  // This function would typically make an AJAX request to load content
  console.log(`Loading content for: ${menuTitle} > ${submenuTitle} > ${thirdLevelTitle}`);
  
  // For demonstration purposes, update the main content area with a placeholder
  const contentArea = document.querySelector('#main-content') || document.querySelector('main');
  if (contentArea) {
    contentArea.innerHTML = `
      <div class="content-header">
        <h1>${thirdLevelTitle || submenuTitle || menuTitle}</h1>
        <nav aria-label="breadcrumb">
          <ol class="breadcrumb">
            <li class="breadcrumb-item">${menuTitle}</li>
            ${submenuTitle ? `<li class="breadcrumb-item">${submenuTitle}</li>` : ''}
            ${thirdLevelTitle ? `<li class="breadcrumb-item active">${thirdLevelTitle}</li>` : ''}
          </ol>
        </nav>
      </div>
      <div class="content-body">
        <div class="placeholder-content">
          <p>Content for ${thirdLevelTitle || submenuTitle || menuTitle} will be displayed here.</p>
        </div>
      </div>
    `;
  }
}

function addProductionBadgeIfNeeded() {
  // Check if we're in production environment
  if (window.location.hostname === '35.226.118.214' || 
      window.location.hostname.includes('prod') || 
      window.location.hostname.includes('production')) {
    const header = document.querySelector('header') || document.body;
    const badge = document.createElement('div');
    badge.className = 'production-badge';
    badge.textContent = 'Production';
    header.appendChild(badge);
  }
} 