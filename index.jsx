import React from 'react';
import ReactDOM from 'react-dom';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import AdminDashboard from './AdminDashboard';

import './styles.css';

const App = () => {
  return (
    <Router>
      <Routes>
        <Route path="/admin/*" element={<AdminDashboard />} />
        <Route path="*" element={<div>Redirecting to admin...</div>} />
      </Routes>
    </Router>
  );
};

ReactDOM.render(<App />, document.getElementById('root')); 