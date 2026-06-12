import React, { useState, useEffect } from 'react';
import Login from './components/auth/Login/Login';
import Register from './components/auth/Register/Register';
import ForgotPassword from './components/auth/ForgotPassword/ForgotPassword';
import ReportList from './components/admin/ReportList/ReportList';
import Sidebar from './components/layout/Sidebar/Sidebar';
import Header from './components/layout/Header/Header';
import TrafficMap from './components/traffic/TrafficMap/TrafficMap';
import UserManagement from './components/admin/UserManagement/UserManagement';
import TrafficAnalytics from './components/admin/TrafficAnalytics/TrafficAnalytics';

import './App.css';

function App() {
  const [user, setUser] = useState(JSON.parse(localStorage.getItem('user')));
  const [authView, setAuthView] = useState('login');
  const [activeTab, setActiveTab] = useState(localStorage.getItem('currentTab') || 'map');

  useEffect(() => {
    localStorage.setItem('currentTab', activeTab);
  }, [activeTab]);

  const isAdmin = user?.vaiTro?.includes('ROLE_ADMIN') ||
                  user?.vaiTro?.toString().includes('ADMIN') ||
                  localStorage.getItem('role') === 'ADMIN';
  const handleLogout = () => {
    localStorage.clear();
    setUser(null);
    setAuthView('login');
    setActiveTab('map');
  };

  if (!user) {
    if (authView === 'login') return <Login onLoginSuccess={() => setUser(JSON.parse(localStorage.getItem('user')))} goToRegister={() => setAuthView('register')} goToForgot={() => setAuthView('forgot')} />;
    if (authView === 'register') return <Register goToLogin={() => setAuthView('login')} />;
    if (authView === 'forgot') return <ForgotPassword goToLogin={() => setAuthView('login')} />;
  }

  return (
    <div className="main-layout">
      <Sidebar user={user} activeTab={activeTab} setActiveTab={setActiveTab} onLogout={handleLogout} />

      <div className="content">
        <Header activeTab={activeTab} user={user} onLogout={handleLogout} onUserUpdate={(u) => { setUser(u); localStorage.setItem('user', JSON.stringify(u)); }} />

        <main className="view-container">
          {activeTab === 'map' && <TrafficMap />}

          {isAdmin && (
            <>
              {activeTab === 'users' && <UserManagement />}
              {activeTab === 'reports' && <ReportList setActiveTab={setActiveTab} />}

              <div style={{ display: activeTab === 'stats' ? 'block' : 'none', height: '100%' }}>
                 {activeTab === 'stats' && <TrafficAnalytics />}
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;