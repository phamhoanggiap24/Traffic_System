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
      console.warn("Xác thực thất bại! Mã lỗi:", error.response.status);

      // 🌟 QUAN TRỌNG NHẤT: Xóa sạch 100% LocalStorage để không bị kẹt user/token cũ gây lặp popup
      localStorage.clear();

      // Bắn sự kiện ép App.js chuyển state user về null ngay lập tức
      window.dispatchEvent(new Event('force-logout'));

      // Hiện thông báo duy nhất một lần
      alert("Phiên đăng nhập đã hết hạn hoặc tài khoản bị thay đổi. Vui lòng đăng nhập lại!");

      // Điều hướng an toàn về trang login
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;