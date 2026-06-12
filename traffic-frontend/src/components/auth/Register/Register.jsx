import React, { useState } from 'react';
import api from '../../../api/axiosConfig';
import { User, Mail, Shield, Key, CheckCircle, AlertCircle, Phone, Save } from 'lucide-react';
import './Register.css';

const Register = ({ goToLogin }) => {
  const [formData, setFormData] = useState({
    hoTen: '',
    email: '',
    soDienThoai: '',
    tenDangNhap: '',
    matKhau: ''
  });

  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);

  const validateFields = (currentData) => {
    // Kiểm tra Họ tên
    if (!currentData.hoTen.trim()) {
      return 'Họ và tên không được để trống!';
    }
    const tenTrimmed = currentData.hoTen.trim();
    if (tenTrimmed.length < 2 || tenTrimmed.length > 50) {
      return 'Họ và tên phải có độ dài từ 2 đến 50 ký tự!';
    }
    const nameRegex = /^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂÂÊÔƠỨỪỬỮỰẤẦẨẪẬẮẰẲẴẶẾỀỂỄỆỐỒỔỖỘỚỜỞỠỢỤỨỪỬỮỰỲÝÝỶỸửữựỳýỵỷỹ\s]+$/;
    if (!nameRegex.test(tenTrimmed)) {
      return 'Họ và tên không được chứa số hoặc ký tự đặc biệt!';
    }

    // Kiểm tra Email
    if (!currentData.email.trim()) {
      return 'Email không được để trống!';
    }
    const emailTrimmed = currentData.email.trim();
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;
    if (!emailRegex.test(emailTrimmed)) {
      return 'Định dạng địa chỉ Email không hợp lệ (Ví dụ: abc@gmail.com)!';
    }
    if (emailTrimmed.endsWith('.co')) {
      return 'Định dạng địa chỉ Email chưa hoàn chỉnh (Ví dụ phải là .com, .vn, .com.vn)!';
    }

    // Kiểm tra Số điện thoại
    if (!currentData.soDienThoai.trim()) {
      return 'Số điện thoại không được để trống!';
    }
    const phoneTrimmed = currentData.soDienThoai.trim();
    const phoneRegex = /^(03|05|07|08|09)\d{8}$/;
    if (!phoneRegex.test(phoneTrimmed)) {
      return 'Số điện thoại phải bắt đầu bằng 03, 05, 07, 08, 09 và gồm đúng 10 chữ số!';
    }

    // Kiểm tra Tên đăng nhập
    if (!currentData.tenDangNhap.trim()) {
      return 'Tên đăng nhập không được để trống!';
    }
    const userTrimmed = currentData.tenDangNhap.trim();
    if (userTrimmed.length < 4) {
      return 'Tên đăng nhập phải chứa ít nhất 4 ký tự!';
    }

    // Kiểm tra Mật khẩu
    if (!currentData.matKhau) {
      return 'Mật khẩu không được để trống!';
    }
    if (currentData.matKhau.length < 6) {
      return 'Mật khẩu phải chứa ít nhất 6 ký tự!';
    }

    return null;
  };

  // XỬ LÝ KHI NGƯỜI DÙNG GÕ VÀO CÁC Ô INPUT
  const handleInputChange = (fieldName, value) => {
    const updatedData = { ...formData, [fieldName]: value };
    setFormData(updatedData);

    const errorMessage = validateFields(updatedData);

    if (!errorMessage) {
      if (message.type === 'error') {
        setMessage({ type: '', text: '' });
      }
    } else {
      setMessage({ type: 'error', text: errorMessage });
    }
  };

  // Xử lý lưu Đăng ký tài khoản mới
  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ type: '', text: '' });

    // Kiểm tra lại toàn bộ form trước khi gửi
    const errorMessage = validateFields(formData);
    if (errorMessage) {
      setMessage({ type: 'error', text: errorMessage });
      return;
    }

    setLoading(true);
    try {
      const res = await api.post('/auth/register', {
        tenDangNhap: formData.tenDangNhap.trim(),
        matKhau: formData.matKhau,
        hoTen: formData.hoTen.trim(),
        email: formData.email.trim(),
        soDienThoai: formData.soDienThoai.trim()
      });

      if (res.data && (res.data.status === 200 || res.data.code === 200)) {
        setMessage({ type: 'success', text: 'Đăng ký thành công! Vui lòng kiểm tra email để kích hoạt.' });

        setTimeout(() => {
          goToLogin();
        }, 2000);
      } else {
        setMessage({ type: 'error', text: res.data.message || 'Đăng ký thất bại!' });
      }
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Lỗi kết nối hệ thống!' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Tạo tài khoản</h2>
        </div>

        <div className="auth-body">
          {message.text && (
            <div className={`profile-alert ${message.type}`}>
              {message.type === 'success' ? <CheckCircle size={16} /> : <AlertCircle size={16} />}
              <span>{message.text}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="info-tab-content">

            <div className="info-group">
              <label><User size={14} /> Họ và tên</label>
              <input
                type="text"
                value={formData.hoTen}
                onChange={(e) => handleInputChange('hoTen', e.target.value)}
                required
              />
            </div>

            <div className="info-group">
              <label><Mail size={14} /> Địa chỉ Email</label>
              <input
                type="email"
                value={formData.email}
                onChange={(e) => handleInputChange('email', e.target.value)}
                required
              />
            </div>

            <div className="info-group">
              <label><Phone size={14} /> Số điện thoại</label>
              <input
                type="text"
                value={formData.soDienThoai}
                onChange={(e) => handleInputChange('soDienThoai', e.target.value)}
                required
              />
            </div>

            <div className="info-group">
              <label><Shield size={14} /> Tên đăng nhập</label>
              <input
                type="text"
                value={formData.tenDangNhap}
                onChange={(e) => handleInputChange('tenDangNhap', e.target.value)}
                required
              />
            </div>

            <div className="info-group">
              <label><Key size={14} /> Mật khẩu</label>
              <input
                type="password"
                value={formData.matKhau}
                onChange={(e) => handleInputChange('matKhau', e.target.value)}
                required
              />
            </div>

            <button type="submit" className="btn-save-password" style={{ background: '#2563eb' }} disabled={loading}>
              <Save size={14} style={{ marginRight: '6px', display: 'inline', verticalAlign: 'middle' }} />
              {loading ? 'Đang xử lý...' : 'Đăng ký ngay'}
            </button>
          </form>
        </div>

        <p className="auth-nav">
          Đã có tài khoản? <span onClick={goToLogin} style={{ cursor: 'pointer' }}>Đăng nhập ngay</span>
        </p>
      </div>
    </div>
  );
};

export default Register;