import axios from 'axios';

const baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: baseURL,
  withCredentials: true
});

// 1. Tự động đính token vào mỗi request gửi đi (Giữ nguyên của bạn)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// File: axiosConfig.js
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && (error.response.status === 403 || error.response.status === 401)) {
      console.warn("Phát hiện tài khoản bị khóa hoặc phiên làm việc hết hạn!");

      // 1. Xóa sạch dấu vết đăng nhập cũ trên bộ nhớ
      localStorage.clear();

      // 2. PHÁT TÍN HIỆU TOÀN HỆ THỐNG: Báo cho App.js biết để reset trạng thái giao diện
      window.dispatchEvent(new Event('force-logout'));

      // 3. Thông báo cho người dùng
      alert("Tài khoản của bạn đã bị khóa hoặc phiên đăng nhập hết hạn. Hệ thống tự động đăng xuất!");

      // 4. Đẩy về trang đăng nhập
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;