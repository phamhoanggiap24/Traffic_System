import React, { useState, useEffect, useRef } from 'react';
import { Bell, User, ChevronDown, LogOut, AlertCircle, Info, Check, Trash2, Menu, X } from 'lucide-react';
import api from '../../../api/axiosConfig';
import Profile from '../../profile/Profile';
import './Header.css';

const Header = ({ activeTab, user, onLogout, onUserUpdate, onMenuToggle, menuOpen }) => {
  const [showDropdown, setShowDropdown] = useState(false);
  const [showNotiDropdown, setShowNotiDropdown] = useState(false);
  const [isProfileOpen, setIsProfileOpen] = useState(false);

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const notiRef = useRef(null);
  const profileRef = useRef(null);

  // Kiểm tra tài khoản hiện tại có phải Admin không
  const isAdmin = user?.vaiTro === 'ROLE_ADMIN' || user?.vaiTro?.toString().includes('ADMIN');

  // LẤY DANH SÁCH THÔNG BÁO THEO TÀI KHOẢN
  const fetchNotifications = async () => {
    if (isAdmin || !user?.taiKhoanId) return;
    try {
      const res = await api.get('/notification', {
        params: {
          taiKhoanId: user.taiKhoanId,
          vaiTro: user.vaiTro || 'ROLE_USER'
        }
      });
      if (res.data && Array.isArray(res.data)) {
        setNotifications(res.data);
        const count = res.data.filter(noti => noti.trangThai === 'CHUA_DOC').length;
        setUnreadCount(count);
      }
    } catch (err) {
      console.error("Lỗi khi tải danh sách thông báo:", err);
    }
  };

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000);
    return () => clearInterval(interval);
  }, [user]);

  // XỬ LÝ KHI BẤM VÀO ĐỌC 1 THÔNG BÁO
  const handleMarkAsRead = async (id, currentStatus) => {
    if (currentStatus === 'DA_DOC') return;
    try {
      const res = await api.put(`/notification/${id}/read`);
      if (res.status === 200) {
        setNotifications(prev =>
          prev.map(noti => noti.canhBaoId === id ? { ...noti, trangThai: 'DA_DOC' } : noti)
        );
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error("Lỗi khi đánh dấu đã đọc:", err);
    }
  };

  // XỬ LÝ ĐỌC TẤT CẢ THÔNG BÁO
  const handleMarkAllAsRead = async () => {
    if (unreadCount === 0) return;
    try {
      const res = await api.put('/notification/read-all', null, {
        params: {
          taiKhoanId: user.taiKhoanId,
          vaiTro: user.vaiTro || 'ROLE_USER'
        }
      });
      if (res.status === 200) {
        setNotifications(prev => prev.map(noti => ({ ...noti, trangThai: 'DA_DOC' })));
        setUnreadCount(0);
      }
    } catch (err) {
      console.error("Lỗi khi đánh dấu đọc tất cả:", err);
    }
  };

  // XỬ LÝ XÓA 1 THÔNG BÁO
  const handleDeleteNotification = async (e, id, currentStatus) => {
    e.stopPropagation();
    try {
      const res = await api.delete(`/notification/${id}`);
      if (res.status === 200) {
        setNotifications(prev => prev.filter(noti => noti.canhBaoId !== id));
        if (currentStatus === 'CHUA_DOC') {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
      }
    } catch (err) {
      console.error("Lỗi khi xóa thông báo:", err);
    }
  };

  // XỬ LÝ XÓA TẤT CẢ THÔNG BÁO
  const handleDeleteAllNotifications = async () => {
    if (notifications.length === 0) return;
    if (!window.confirm("Bạn có chắc chắn muốn xóa toàn bộ thông báo không?")) return;
    try {
      const res = await api.delete('/notification/delete-all', {
        params: {
          taiKhoanId: user.taiKhoanId,
          vaiTro: user.vaiTro || 'ROLE_USER'
        }
      });
      if (res.status === 200) {
        setNotifications([]);
        setUnreadCount(0);
      }
    } catch (err) {
      console.error("Lỗi khi xóa tất cả thông báo:", err);
    }
  };

  // Click ra ngoài màn hình thì tự động đóng menu Dropdown
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (notiRef.current && !notiRef.current.contains(event.target)) {
        setShowNotiDropdown(false);
      }
      if (profileRef.current && !profileRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const getHeaderTitle = () => {
    switch (activeTab) {
      case 'map': return 'Bản đồ giao thông trực tuyến';
      case 'stats': return 'Thống kê & Phân tích';
      case 'users': return 'Quản lý tài khoản';
      default: return 'Hệ thống giao thông';
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) + ' - ' + date.toLocaleDateString('vi-VN');
  };

  return (
    <header className="header-container">
      <div className="header-top">
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button 
            className="header-menu-toggle" 
            onClick={onMenuToggle}
            title={menuOpen ? 'Đóng menu' : 'Mở menu'}
          >
            {menuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
          <h2 className="header-title"style={{
              color: activeTab === 'map' ? '#2563eb' : 'inherit', fontWeight: '600'
          }}>
          {getHeaderTitle()}</h2>
        </div>

        <div className="header-right-actions">

          {/* CHUÔNG THÔNG BÁO: Chỉ hiển thị khi không phải là Admin */}
          {!isAdmin && (
            <div className="notification-wrapper" ref={notiRef}>
              <div className="bell-trigger" onClick={() => setShowNotiDropdown(!showNotiDropdown)}>
                <Bell size={22} className="icon-btn" />
                {unreadCount > 0 && (
                  <span className="notification-badge">{unreadCount}</span>
                )}
              </div>

              {/* Menu Dropdown danh sách thông báo */}
              {showNotiDropdown && (
                <div className="noti-dropdown">
                  <div className="noti-dropdown-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <h4>Thông báo hệ thống</h4>
                      {unreadCount > 0 && <span className="noti-count-tag">Chưa đọc: {unreadCount}</span>}
                    </div>
                    {/* Đọc tất cả / Xóa tất cả */}
                    <div className="noti-header-actions" style={{ display: 'flex', gap: '8px' }}>
                      {unreadCount > 0 && (
                        <button className="noti-action-btn" onClick={handleMarkAllAsRead} title="Đánh dấu đã đọc tất cả" style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#2563eb' }}>
                          <Check size={16} />
                        </button>
                      )}
                      {notifications.length > 0 && (
                        <button className="noti-action-btn" onClick={handleDeleteAllNotifications} title="Xóa tất cả thông báo" style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#dc2626' }}>
                          <Trash2 size={16} />
                        </button>
                      )}
                    </div>
                  </div>

                  <div className="noti-dropdown-body">
                    {notifications.length === 0 ? (
                      <div className="noti-empty-state">
                        <Info size={28} />
                        <p>Bạn không có thông báo nào mới</p>
                      </div>
                    ) : (
                      notifications.map((noti) => (
                        <div
                          key={noti.canhBaoId}
                          className={`noti-item ${noti.trangThai === 'CHUA_DOC' ? 'unread' : 'read'}`}
                          onClick={() => handleMarkAsRead(noti.canhBaoId, noti.trangThai)}
                          style={{ display: 'flex', alignItems: 'center', stroke: 'none', justifyContent: 'space-between', position: 'relative' }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', flex: 1 }}>
                            <div className="noti-icon-box">
                              {noti.loaiCanhBao === 'NGUY_HIEM' || noti.loaiCanhBao === 'KET_XE' ? (
                                <AlertCircle size={16} className="danger-icon" />
                              ) : (
                                <Info size={16} className="info-icon" />
                              )}
                            </div>
                            <div className="noti-content-box" style={{ marginRight: '40px' }}>
                              <p className="noti-text">{noti.noiDung}</p>
                              <span className="noti-time">{formatTime(noti.thoiGianGui)}</span>
                            </div>
                          </div>

                          {/* Các nút bấm hành động */}
                          <div className="noti-item-actions" style={{ display: 'flex', alignItems: 'center', gap: '6px', position: 'absolute', right: '12px' }}>
                            {noti.trangThai === 'CHUA_DOC' && (
                              <div className="unread-dot" style={{ position: 'static' }}></div>
                            )}
                            <button
                              className="noti-delete-single-btn"
                              onClick={(e) => handleDeleteNotification(e, noti.canhBaoId, noti.trangThai)}
                              title="Xóa thông báo này"
                              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#aaa', padding: '2px' }}
                            >
                              <Trash2 size={14} className="hover-danger" />
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TÀI KHOẢN CÁ NHÂN*/}
          <div className="profile-section" ref={profileRef} onClick={() => setShowDropdown(!showDropdown)}>
            <div className="user-avatar-circle">
              <User size={20} />
            </div>
            <div className="user-text">
              <span className="user-name">{user?.hoTen || 'Người dùng'}</span>
              <span className="user-role">
                {isAdmin ? 'Quản trị viên' : 'Thành viên'}
              </span>
            </div>
            <ChevronDown size={16} className={`arrow-icon ${showDropdown ? 'rotate' : ''}`} />

            {showDropdown && (
              <div className="profile-dropdown" onClick={(e) => e.stopPropagation()}>
                <div
                  className="dropdown-item"
                  onClick={() => {
                    setIsProfileOpen(true);
                    setShowDropdown(false);
                  }}
                >
                  <User size={16} /> <span>Thông tin cá nhân</span>
                </div>
                <div className="dropdown-divider"></div>
                <div className="dropdown-item logout-text" onClick={onLogout}>
                  <LogOut size={16} /> <span>Đăng xuất</span>


                </div>
              </div>
            )}
          </div>

        </div>
      </div>

      <Profile
        isOpen={isProfileOpen}
        onClose={() => setIsProfileOpen(false)}
        currentUser={user}
        onUserUpdate={onUserUpdate}
      />
    </header>
  );
};

export default Header;