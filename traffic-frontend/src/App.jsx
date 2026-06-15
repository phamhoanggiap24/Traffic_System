import React, { useState, useEffect } from 'react';
import api from './api/axiosConfig'; // Đảm bảo đường dẫn tới file axiosConfig chính xác
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
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [pendingRefreshTrigger, setPendingRefreshTrigger] = useState(0);

  // 🌟 VÒNG QUÉT KIỂM TRA TRẠNG THÁI TOÀN HỆ THỐNG (BẤT KỂ ĐANG Ở TAB NÀO)
  useEffect(() => {
    // Nếu chưa đăng nhập hoặc không có user thì tuyệt đối không quét ngầm (Tránh lặp popup ở màn Login)
    if (!user) return;

    const checkAccountStatus = async () => {
      try {
        // Gửi request siêu nhẹ lên endpoint kiểm tra trạng thái bảo mật ở backend
        await api.get('/profile/me');
      } catch (error) {
        console.error("Phát hiện tài khoản vi phạm hoặc hết hạn phiên làm việc ngầm:", error);
        // Khi lỗi 403/401 xảy ra, Axios Interceptor sẽ tự động xử lý xóa localStorage và redirect về /login.
      }
    };

    // 1. Chạy kiểm tra ngay lập tức khi người dùng vừa load trang hoặc chuyển tab
    checkAccountStatus();

    // 2. Thiết lập chạy ngầm định kỳ cứ mỗi 20 giây quét một lần (20000ms)
    const intervalId = setInterval(checkAccountStatus, 20000);

    // Dọn dẹp luồng chạy ngầm khi người dùng logout hoặc hủy component
    return () => clearInterval(intervalId);
  }, [user]); // Theo dõi trạng thái biến user


  useEffect(() => {
    localStorage.setItem('currentTab', activeTab);
  }, [activeTab]);

  // Đóng sidebar khi chuyển tab
  useEffect(() => {
    setSidebarOpen(false);
  }, [activeTab]);

  // Theo dõi sự kiện thay đổi báo cáo từ ReportList để cập nhật trigger
  useEffect(() => {
    const handleIncidentUpdate = () => {
      setPendingRefreshTrigger(prev => prev + 1);
    };

    window.addEventListener('incident-verified', handleIncidentUpdate);
    return () => {
      window.removeEventListener('incident-verified', handleIncidentUpdate);
    };
  }, []);

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
      {/* Overlay khi sidebar mở trên mobile */}
      <div
        className={`sidebar-overlay ${!sidebarOpen ? 'hidden' : ''}`}
        onClick={() => setSidebarOpen(false)}
      />

      <Sidebar
        user={user}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onLogout={handleLogout}
        isOpen={sidebarOpen}
        pendingRefreshTrigger={pendingRefreshTrigger}
      />

      <div className="content">
        <Header 
          activeTab={activeTab} 
          user={user} 
          onLogout={handleLogout} 
          onUserUpdate={(u) => { setUser(u); localStorage.setItem('user', JSON.stringify(u)); }}
          onMenuToggle={() => setSidebarOpen(!sidebarOpen)}
          menuOpen={sidebarOpen}
        />

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