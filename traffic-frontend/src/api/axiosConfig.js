import axios from 'axios';

const baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL,
  withCredentials: true
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || '';

    const isAuthRequest =
      requestUrl.includes('/auth/login') ||
      requestUrl.includes('/auth/register') ||
      requestUrl.includes('/auth/forgot-password') ||
      requestUrl.includes('/auth/reset-password') ||
      requestUrl.includes('/auth/verify') ||
      requestUrl.includes('/auth/refresh-token') ||
      requestUrl.includes('/profile/location');

    if ((status === 401 || status === 403) && !isAuthRequest) {
      console.warn('Phiên đăng nhập không hợp lệ:', status);

      localStorage.clear();

      window.dispatchEvent(new Event('force-logout'));

      alert('Phiên đăng nhập đã hết hạn hoặc tài khoản bị thay đổi. Vui lòng đăng nhập lại!');

      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export default api;