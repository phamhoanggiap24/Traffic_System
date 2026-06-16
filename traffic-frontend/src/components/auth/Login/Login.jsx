import React, { useState } from 'react';
import './Login.css';
import api from '../../../api/axiosConfig';

const Login = ({ onLoginSuccess, goToRegister, goToForgot }) => {
  const [credentials, setCredentials] = useState({ tenDangNhap: '', matKhau: '' });

  const handleLogin = async (e) => {
    e.preventDefault();
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('role');

    try {
      const res = await api.post('/auth/login', credentials);

      if (res.data.status === 200) {
        localStorage.setItem('accessToken', res.data.data.accessToken);
        localStorage.setItem('refreshToken', res.data.data.refreshToken);
        localStorage.setItem('user', JSON.stringify(res.data.data.user));

        // Hiển thị thông báo
        console.log(res.data.message);
        onLoginSuccess();
      } else {
        alert(res.data.message);
      }
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        alert(err.response.data.message);
      } else {
        alert("Không thể kết nối đến máy chủ. Vui lòng thử lại sau!");
      }
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Đăng Nhập</h2>
        </div>
        <form onSubmit={handleLogin}>
          <div className="input-group">
            <label>Tên đăng nhập</label>
            <input
              type="text"
              required
              value={credentials.tenDangNhap}
              onChange={e => setCredentials({...credentials, tenDangNhap: e.target.value})}
            />
          </div>
          <div className="input-group">
            <label>Mật khẩu</label>
            <input
              type="password"
              required
              value={credentials.matKhau}
              onChange={e => setCredentials({...credentials, matKhau: e.target.value})}
            />
          </div>
          <button type="submit" className="auth-btn">Đăng nhập</button>
        </form>
        <div className="auth-footer">
          <span onClick={goToForgot} style={{ cursor: 'pointer' }}>Quên mật khẩu</span>
          <span onClick={goToRegister} style={{ cursor: 'pointer' }}>Đăng ký ngay</span>
        </div>
      </div>
    </div>
  );
};

export default Login;