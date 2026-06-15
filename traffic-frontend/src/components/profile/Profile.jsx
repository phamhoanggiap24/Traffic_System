import React, { useState, useEffect } from 'react';
import api from '../../api/axiosConfig';
import { X, User, Mail, Shield, Key, CheckCircle, AlertCircle, Phone, Save, Award } from 'lucide-react';
import './Profile.css';

const Profile = ({ isOpen, onClose, currentUser, onUserUpdate }) => {
  const [formData, setFormData] = useState({
    hoTen: '',
    soDienThoai: ''
  });
  const [passwordData, setPasswordData] = useState({ matKhauCu: '', matKhauMoi: '', confirmPassword: '' });
  const [activeTab, setActiveTab] = useState('info');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);

  // BỔ SUNG STATE: Để lưu trữ thông tin "tươi" vừa cập nhật từ Database
  const [liveProfile, setLiveProfile] = useState(null);

  // VÒNG ĐỜI KHI MỞ MODAL PROFILE
  useEffect(() => {
    if (!isOpen || !currentUser) return;

    const loadProfile = async () => {
      try {
        const res = await api.get('/profile/me');

        if (res.data?.status === 200) {
          setLiveProfile(res.data.data);
        } else {
          setLiveProfile(currentUser);
        }
      } catch {
        setLiveProfile(currentUser);
      }
    };

    setFormData({
      hoTen: currentUser.hoTen || '',
      soDienThoai: currentUser.soDienThoai || ''
    });

    setMessage({ type: '', text: '' });
    setActiveTab('info');
    setPasswordData({
      matKhauCu: '',
      matKhauMoi: '',
      confirmPassword: ''
    });

    loadProfile();

    const interval = setInterval(loadProfile, 5000);

    return () => clearInterval(interval);
  }, [isOpen, currentUser]);

  // Xác định tài khoản có phải là Admin không (Sử dụng dữ liệu live nếu có)
  const userForCheck = liveProfile || currentUser;
  const isAdminUser = userForCheck?.vaiTro === 'ROLE_ADMIN' || userForCheck?.vaiTro?.toString().includes('ADMIN');

  // HÀM KIỂM TRA ĐỊA CHỈ HỢP LỆ
  const validateFields = (currentData) => {
    const tenTrimmed = currentData.hoTen.trim();
    if (tenTrimmed.length < 2 || tenTrimmed.length > 50) {
      return 'Họ và tên phải có độ dài từ 2 đến 50 ký tự!';
    }
    const nameRegex = /^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂÂÊÔƠỨỪỬỮỰẤẦẨẪẬẮẰẲẴẶẾỀỂỄỆỐỒỔỖỘỚỜỞỠỢỤỨỪỬỮỰỲÝÝỶỸửữựỳýỵỷỹ\s]+$/;
    if (!nameRegex.test(tenTrimmed)) {
      return 'Họ và tên không được chứa số hoặc ký tự đặc biệt!';
    }

    if (currentData.soDienThoai && currentData.soDienThoai.trim() !== '') {
      const phoneTrimmed = currentData.soDienThoai.trim();
      const phoneRegex = /^(03|05|07|08|09)\d{8}$/;
      if (!phoneRegex.test(phoneTrimmed)) {
        return 'Số điện thoại phải bắt đầu bằng 03, 05, 07, 08, 09 và gồm đúng 10 chữ số!';
      }
    }
    return null;
  };

  const handleInputChange = (fieldName, value) => {
    const updatedData = { ...formData, [fieldName]: value };
    setFormData(updatedData);

    const errorMessage = validateFields(updatedData);
    if (!errorMessage) {
      if (message.type === 'error') setMessage({ type: '', text: '' });
    } else {
      setMessage({ type: 'error', text: errorMessage });
    }
  };

  if (!isOpen) return null;

  // Xử lý lưu Cập nhật thông tin cá nhân
  const handleUpdateInfo = async (e) => {
    e.preventDefault();
    setMessage({ type: '', text: '' });

    const errorMessage = validateFields(formData);
    if (errorMessage) {
      setMessage({ type: 'error', text: errorMessage });
      return;
    }

    setLoading(true);
    try {
      const res = await api.put('/profile/update-info', {
        hoTen: formData.hoTen.trim(),
        soDienThoai: formData.soDienThoai.trim()
      });

      if (res.data && res.data.status === 200) {
        setMessage({ type: 'success', text: 'Cập nhật thông tin thành công!' });

        // Đồng bộ dữ liệu ra bên ngoài hệ thống cho các component khác cùng biết
        if (onUserUpdate) {
          onUserUpdate({
            ...userForCheck,
            hoTen: formData.hoTen.trim(),
            soDienThoai: formData.soDienThoai.trim()
          });
        }
      } else {
        setMessage({ type: 'error', text: res.data.message || 'Cập nhật thất bại!' });
      }
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Lỗi kết nối hệ thống!' });
    } finally {
      setLoading(false);
    }
  };

  // Xử lý đổi mật khẩu
  const handlePasswordChange = async (e) => {
    e.preventDefault();
    if (passwordData.matKhauMoi !== passwordData.confirmPassword) {
      setMessage({ type: 'error', text: 'Mật khẩu mới không trùng khớp!' });
      return;
    }

    setLoading(true);
    setMessage({ type: '', text: '' });

    try {
      const res = await api.post('/profile/change-password', {
        tenDangNhap: userForCheck?.tenDangNhap,
        matKhauCu: passwordData.matKhauCu,
        matKhauMoi: passwordData.matKhauMoi
      });

      if (res.data && res.data.status === 200) {
        setMessage({ type: 'success', text: 'Đổi mật khẩu thành công!' });
        setPasswordData({ matKhauCu: '', matKhauMoi: '', confirmPassword: '' });
      } else {
        setMessage({ type: 'error', text: res.data.message || 'Có lỗi xảy ra!' });
      }
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Mật khẩu cũ không chính xác!' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="profile-modal-overlay" onClick={onClose}>
      <div className="profile-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="profile-modal-header">
          <h3>Thông tin tài khoản</h3>
          <button className="close-btn" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="profile-modal-tabs">
          <button
            className={`tab-btn ${activeTab === 'info' ? 'active' : ''}`}
            onClick={() => setActiveTab('info')}
          >
            Thông tin cơ bản
          </button>
          <button
            className={`tab-btn ${activeTab === 'password' ? 'active' : ''}`}
            onClick={() => setActiveTab('password')}
          >
            Đổi mật khẩu
          </button>
        </div>

        <div className="profile-modal-body">
          {message.text && (
            <div className={`profile-alert ${message.type}`}>
              {message.type === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
              <span>{message.text}</span>
            </div>
          )}

          {activeTab === 'info' ? (
            <form onSubmit={handleUpdateInfo} className="info-tab-content">
              <div className="info-group">
                <label><User size={14} /> Tên đăng nhập</label>
                <input type="text" value={userForCheck?.tenDangNhap || 'N/A'} disabled />
              </div>

              <div className="info-group">
                <label><Mail size={14} /> Địa chỉ Email</label>
                <input
                  type="text"
                  value={userForCheck?.email || 'N/A'}
                  disabled
                />
              </div>

              <div className="info-group">
                <label><Shield size={14} /> Vai trò</label>
                <input
                  type="text"
                  value={isAdminUser ? 'Quản trị viên hệ thống' : 'Thành viên'}
                  disabled
                />
              </div>

              {/* 🌟 ĐÃ ĐỔI: Sử dụng điểm uy tín từ "liveProfile" đọc trực tiếp tại DB */}
              <div className="info-group">
                <label><Award size={14} /> Độ tin cậy</label>
                <input
                  type="text"
                  value={
                    isAdminUser
                      ? '(N/A)'
                      : `${userForCheck?.doTinCayNguoiDung ?? 50}/50`
                  }
                  disabled
                  style={{
                    fontWeight: '600',
                    color: isAdminUser
                      ? '#64748b'
                      : (userForCheck?.doTinCayNguoiDung >= 40 ? '#10b981' : userForCheck?.doTinCayNguoiDung >= 20 ? '#f59e0b' : '#ef4444'),
                    background: '#f8fafc'
                  }}
                />
              </div>

              <div className="info-group">
                <label>Họ và tên</label>
                <input
                  type="text"
                  value={formData.hoTen}
                  onChange={(e) => handleInputChange('hoTen', e.target.value)}
                  required
                />
              </div>

              <div className="info-group">
                <label><Phone size={14} /> Số điện thoại</label>
                <input
                  type="text"
                  value={formData.soDienThoai}
                  onChange={(e) => handleInputChange('soDienThoai', e.target.value)}
                />
              </div>
              <button type="submit" className="btn-save-password" style={{background: '#2563eb'}} disabled={loading}>
                <Save size={14} style={{marginRight: '6px', display: 'inline', verticalAlign: 'middle'}} />
                {loading ? 'Đang xử lý...' : 'Lưu thay đổi'}
              </button>
            </form>
          ) : activeTab === 'password' ? (
            <form onSubmit={handlePasswordChange} className="password-tab-content">
              <div className="info-group">
                <label><Key size={14} /> Mật khẩu hiện tại</label>
                <input
                  type="password"
                  required
                  value={passwordData.matKhauCu}
                  onChange={(e) => setPasswordData({...passwordData, matKhauCu: e.target.value})}
                />
              </div>
              <div className="info-group">
                <label><Key size={14} /> Mật khẩu mới</label>
                <input
                  type="password"
                  required
                  minLength={6}
                  value={passwordData.matKhauMoi}
                  onChange={(e) => setPasswordData({...passwordData, matKhauMoi: e.target.value})}
                />
              </div>
              <div className="info-group">
                <label><Key size={14} /> Xác nhận mật khẩu mới</label>
                <input
                  type="password"
                  required
                  value={passwordData.confirmPassword}
                  onChange={(e) => setPasswordData({...passwordData, confirmPassword: e.target.value})}
                />
              </div>
              <button type="submit" className="btn-save-password" disabled={loading}>
                {loading ? 'Đang xử lý...' : 'Cập nhật mật khẩu'}
              </button>
            </form>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default Profile;