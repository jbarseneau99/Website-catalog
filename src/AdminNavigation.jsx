import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import adminHierarchy from './adminHierarchy.json';
import logoImage from './33fg_Logo-black.png';

// Convert spaces to dashes and make lowercase for URL-friendly paths
const formatPath = (path) => {
  return path.toLowerCase().replace(/\s+/g, '-');
};

// Recursive component to render menu items with their children
const MenuItem = ({ item, level = 0, expanded = {}, toggleExpand }) => {
  const hasChildren = item.children && item.children.length > 0;
  const isExpanded = expanded[item.name] || false;
  const path = formatPath(item.name);
  const indentStyle = { paddingLeft: `${level * 16}px` };

  return (
    <>
      <li className="nav-item">
        <div 
          className={`nav-link ${hasChildren ? 'has-children' : ''} ${level === 0 ? 'top-level' : ''}`} 
          style={indentStyle}
        >
          {hasChildren && (
            <button 
              className={`expand-btn ${isExpanded ? 'expanded' : ''}`}
              onClick={() => toggleExpand(item.name)}
            >
              {isExpanded ? '▼' : '►'}
            </button>
          )}
          <Link to={`/admin/${path}`}>{item.name}</Link>
        </div>
      </li>
      
      {hasChildren && isExpanded && (
        <ul className="nav-submenu">
          {item.children.map((child, idx) => (
            <MenuItem 
              key={`${child.name}-${idx}`} 
              item={child} 
              level={level + 1}
              expanded={expanded}
              toggleExpand={toggleExpand}
            />
          ))}
        </ul>
      )}
    </>
  );
};

const AdminNavigation = () => {
  const [expanded, setExpanded] = useState({});

  const toggleExpand = (itemName) => {
    setExpanded(prev => ({
      ...prev,
      [itemName]: !prev[itemName]
    }));
  };

  return (
    <div className="admin-navigation">
      <div className="logo-container">
        <img src={logoImage} alt="Mach33 Logo" className="admin-logo" />
      </div>
      
      <nav className="admin-nav">
        <ul className="nav-menu">
          {adminHierarchy.children.map((item, idx) => (
            <MenuItem 
              key={`${item.name}-${idx}`} 
              item={item} 
              expanded={expanded}
              toggleExpand={toggleExpand}
            />
          ))}
        </ul>
      </nav>
      
      <style jsx>{`
        .admin-navigation {
          width: 260px;
          height: 100vh;
          background-color: #f8f9fa;
          border-right: 1px solid #e9ecef;
          display: flex;
          flex-direction: column;
          overflow-y: auto;
        }
        
        .logo-container {
          padding: 20px;
          display: flex;
          justify-content: center;
          border-bottom: 1px solid #e9ecef;
        }
        
        .admin-logo {
          max-width: 80px;
          height: auto;
        }
        
        .admin-nav {
          flex: 1;
        }
        
        .nav-menu, .nav-submenu {
          list-style: none;
          padding: 0;
          margin: 0;
        }
        
        .nav-item {
          margin: 0;
        }
        
        .nav-link {
          display: flex;
          align-items: center;
          padding: 10px 15px;
          color: #343a40;
          transition: background-color 0.2s;
          position: relative;
        }
        
        .nav-link:hover {
          background-color: #e9ecef;
        }
        
        .nav-link.top-level {
          font-weight: 600;
        }
        
        .expand-btn {
          background: none;
          border: none;
          font-size: 10px;
          color: #6c757d;
          cursor: pointer;
          padding: 0;
          margin-right: 8px;
          width: 16px;
          height: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        
        .nav-link a {
          text-decoration: none;
          color: inherit;
        }
      `}</style>
    </div>
  );
};

export default AdminNavigation; 