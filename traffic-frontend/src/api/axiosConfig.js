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

// 2. BỔ SUNG: Tự động bắt lỗi từ Server trả về để thực hiện Force Logout
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Nếu máy chủ trả về mã lỗi 403 (Bị cấm) hoặc 401 (Hết hạn phiên)
    if (error.response && (error.response.status === 403 || error.response.status === 401)) {
      console.warn("Phát hiện tài khoản bị khóa hoặc phiên làm việc hết hạn!");

      // Xóa sạch dấu vết đăng nhập cũ trên trình duyệt
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');

      // Thông báo cho người dùng biết lý do
      alert("Tài khoản của bạn đã bị khóa hoặc phiên đăng nhập hết hạn. Hệ thống tự động đăng xuất!");

      // Đẩy văng người dùng về trang Login ngay lập tức
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;