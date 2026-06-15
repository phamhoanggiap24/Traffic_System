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

  // 1. LẮNG NGHE SỰ KIỆN BUỘC ĐĂNG XUẤT TỪ AXIOS INTERCEPTOR
  useEffect(() => {
    const handleForceLogout = () => {
      setUser(null);          // Sút văng khỏi màn hình chính, ép về màn Login ngay lập tức
      setAuthView('login');   // Đảm bảo đưa view về khung Đăng nhập
      setActiveTab('map');    // Reset tab mặc định
    };

    window.addEventListener('force-logout', handleForceLogout);
    return () => {
      window.removeEventListener('force-logout', handleForceLogout);
    };
  }, []);

  // 2. VÒNG QUÉT KIỂM TRA TRẠNG THÁI NGẦM (CHỈ CHẠY KHI ĐỦ ĐIỀU KIỆN)
  useEffect(() => {
    // ĐIỀU KIỆN CHẶN BẢO VỆ: Nếu thiếu user HOẶC thiếu hẳn accessToken trong máy thì dừng toàn bộ luồng quét!
    const token = localStorage.getItem('accessToken');
    if (!user || !token) return;

    const checkAccountStatus = async () => {
      try {
        const res = await api.get('/profile/me');

        if (res.data?.status === 200 && res.data?.data) {
          const oldUser = JSON.parse(localStorage.getItem('user')) || {};

          const updatedUser = {
            ...oldUser,
            ...res.data.data,
            vaiTro: oldUser.vaiTro
          };

          if (
            oldUser.doTinCayNguoiDung !== updatedUser.doTinCayNguoiDung ||
            oldUser.trangThai !== updatedUser.trangThai
          ) {
            localStorage.setItem('user', JSON.stringify(updatedUser));
            setUser(updatedUser);
          }
        }
      } catch (error) {
        console.error("Lỗi xác thực hệ thống ngầm:", error);
      }
    };

    // Tạo một khoảng hoãn nhẹ 1 giây sau khi mount mới check phát đầu tiên để tránh xung đột
    const initialTimeout = setTimeout(checkAccountStatus, 1000);

    // Thiết lập chạy ngầm định kỳ mỗi 5 giây một lần
    const intervalId = setInterval(checkAccountStatus, 5000);

    // DỌN DẸP TUYỆT ĐỐI: Khi user = null, dọn dẹp sạch tiến trình tránh rò rỉ request
    return () => {
      clearTimeout(initialTimeout);
      clearInterval(intervalId);
    };
  }, [user?.tenDangNhap]); // Theo dõi sát sao biến user


  // --- TOÀN BỘ LOGIC PHÍA DƯỚI GIỮ NGUYÊN CŨ CỦA BẠN ---
  useEffect(() => {
    localStorage.setItem('currentTab', activeTab);
  }, [activeTab]);

  useEffect(() => {
    setSidebarOpen(false);
  }, [activeTab]);

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
      <div className={`sidebar-overlay ${!sidebarOpen ? 'hidden' : ''}`} onClick={() => setSidebarOpen(false)} />
      <Sidebar user={user} activeTab={activeTab} setActiveTab={setActiveTab} onLogout={handleLogout} isOpen={sidebarOpen} pendingRefreshTrigger={pendingRefreshTrigger} />
      <div className="content">
        <Header activeTab={activeTab} user={user} onLogout={handleLogout} onUserUpdate={(u) => { setUser(u); localStorage.setItem('user', JSON.stringify(u)); }} onMenuToggle={() => setSidebarOpen(!sidebarOpen)} menuOpen={sidebarOpen} />
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