import React, { useState, useEffect } from 'react';
import './Sidebar.css';
import { Map, BarChart3, Users, ShieldAlert } from 'lucide-react';
import api from '../../../api/axiosConfig';

const Sidebar = ({ user, activeTab, setActiveTab, onLogout, isOpen, pendingRefreshTrigger }) => {
  const isAdmin = user?.vaiTro?.includes('ROLE_ADMIN') ||
                  user?.vaiTro?.toString().includes('ADMIN') ||
                  localStorage.getItem('role') === 'ADMIN';
  // State lưu số lượng báo cáo chờ duyệt
  const [pendingCount, setPendingCount] = useState(0);

  // Gọi API lấy số lượng báo cáo chưa duyệt
  const fetchPendingCount = async () => {
    if (!isAdmin) return;
    try {
      const res = await api.get('/report/admin/pending-count');

      if (res && res.data) {
        console.log("Dữ liệu trả về:", res.data);

        let countValue = 0;

        // Nếu Backend trả về object có chứa trường 'result'
        if (res.data.result !== undefined && res.data.result !== null) {
          countValue = res.data.result;
        }
        // Nếu Backend trả về object có chứa trường 'data'
        else if (res.data.data !== undefined && res.data.data !== null) {
          countValue = res.data.data;
        }
        // Nếu dữ liệu trả về nằm thẳng trong res.data
        else {
          countValue = res.data;
        }

        // Chuyển đổi về kiểu số nguyên chính xác
        const parsedCount = parseInt(countValue, 10);

        // Cập nhật State nếu là một số hợp lệ
        if (!isNaN(parsedCount)) {
          setPendingCount(parsedCount);
        }
      }
    } catch (err) {
      console.error("Lỗi khi lấy số lượng báo cáo chờ duyệt:", err);
    }
  };

  // Tự động tải lại dữ liệu sau mỗi 15 giây HOẶC tải lại ngay khi nhận tín hiệu từ App.js
  useEffect(() => {
    if (isAdmin) {
      fetchPendingCount();
      const interval = setInterval(fetchPendingCount, 15000);
      return () => clearInterval(interval);
    }
  }, [user, isAdmin, pendingRefreshTrigger]); // Bổ sung theo dõi thay đổi dữ liệu báo cáo

  // Theo dõi ReportList truyền sang để cập nhật số Badge
  useEffect(() => {
    const handleUpdateBadge = () => fetchPendingCount();
    window.addEventListener('incident-verified', handleUpdateBadge);
    return () => window.removeEventListener('incident-verified', handleUpdateBadge);
  }, [isAdmin]);

  useEffect(() => {
    if (!isAdmin && (activeTab === 'reports' || activeTab === 'users' || activeTab === 'stats')) {
      setActiveTab('map');
    }
  }, [isAdmin, activeTab, setActiveTab]);

  return (
    <aside className={`sidebar ${isAdmin ? 'admin-mode' : ''} ${isOpen ? 'open' : ''}`}>
      <div className="sidebar-logo">
        TRAFFIC MAP
      </div>

      <nav className="sidebar-nav">
        <button className={activeTab === 'map' ? 'active' : ''} onClick={() => setActiveTab('map')}>
          <Map size={20} /> <span>Bản đồ</span>
        </button>

        {isAdmin && (
          <>
            {/* Quản lý người dùng */}
            <button className={activeTab === 'users' ? 'active' : ''} onClick={() => setActiveTab('users')}>
              <Users size={20} /> <span>Quản lý người dùng</span>
            </button>

            {/* Danh sách báo cáo kèm số đếm */}
            <button
              className={activeTab === 'reports' ? 'active' : ''}
              onClick={() => setActiveTab('reports')}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                <ShieldAlert size={20} />
                <span>Danh sách báo cáo</span>
              </div>

              {pendingCount > 0 ? (
                <span className="sidebar-badge-count">{pendingCount}</span>
              ) : (
                <span className="sidebar-badge-count" style={{ opacity: 0.7, backgroundColor: '#475569' }}>0</span>
              )}
            </button>

            <button className={activeTab === 'stats' ? 'active' : ''} onClick={() => setActiveTab('stats')}>
              <BarChart3 size={20} /> <span>Thống kê</span>
            </button>
          </>
        )}

        <div className="nav-spacer"></div>
      </nav>
    </aside>
  );
};

export default Sidebar;