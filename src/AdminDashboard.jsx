import React from 'react';
import { Routes, Route } from 'react-router-dom';
import AdminNavigation from './AdminNavigation';

// Placeholder component for admin routes
const AdminPlaceholder = ({ title }) => (
  <div className="admin-content-placeholder">
    <h2>{title}</h2>
    <p>This is a placeholder for the {title} section.</p>
  </div>
);

const AdminDashboard = () => {
  return (
    <div className="admin-dashboard">
      <AdminNavigation />
      
      <div className="admin-content">
        <header className="admin-header">
          <h1>Mach33 Fund Accounting</h1>
          <div className="admin-actions">
            <button className="admin-button">Settings</button>
            <button className="admin-button">Profile</button>
          </div>
        </header>
        
        <main className="admin-main">
          <Routes>
            <Route path="/" element={<AdminPlaceholder title="Dashboard" />} />
            <Route path="/dashboard" element={<AdminPlaceholder title="Dashboard" />} />
            <Route path="/agent-ecosystem" element={<AdminPlaceholder title="Agent Ecosystem" />} />
            <Route path="/language-intelligence" element={<AdminPlaceholder title="Language Intelligence" />} />
            <Route path="/knowledge-management" element={<AdminPlaceholder title="Knowledge Management" />} />
            <Route path="/integration-hub" element={<AdminPlaceholder title="Integration Hub" />} />
            <Route path="/infrastructure" element={<AdminPlaceholder title="Infrastructure" />} />
            
            {/* Example of deep linking for a third-level item */}
            <Route 
              path="/agent-ecosystem/agent-health-monitor/status-dashboard" 
              element={<AdminPlaceholder title="Agent Status Dashboard" />} 
            />
            
            {/* Add more routes as needed */}
            <Route path="*" element={<AdminPlaceholder title="Not Found" />} />
          </Routes>
        </main>
      </div>
      
      <style jsx>{`
        .admin-dashboard {
          display: flex;
          min-height: 100vh;
          background-color: #f8f9fa;
        }
        
        .admin-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          overflow: hidden;
        }
        
        .admin-header {
          height: 64px;
          background-color: white;
          border-bottom: 1px solid #e9ecef;
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 0 24px;
        }
        
        .admin-header h1 {
          margin: 0;
          font-size: 20px;
          color: #343a40;
        }
        
        .admin-actions {
          display: flex;
          gap: 12px;
        }
        
        .admin-button {
          padding: 8px 16px;
          background-color: #f8f9fa;
          border: 1px solid #dee2e6;
          border-radius: 4px;
          cursor: pointer;
        }
        
        .admin-button:hover {
          background-color: #e9ecef;
        }
        
        .admin-main {
          flex: 1;
          padding: 24px;
          overflow-y: auto;
        }
        
        .admin-content-placeholder {
          background-color: white;
          border-radius: 8px;
          padding: 24px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
        }
      `}</style>
    </div>
  );
};

export default AdminDashboard; 