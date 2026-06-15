import React, { useState, useEffect, useRef } from 'react';
import './ForgotPassword.css';
import api from '../../../api/axiosConfig';

const ForgotPassword = ({ goToLogin }) => {
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  // DÙNG REFS ĐỂ LƯU MỐC THỜI GIAN KẾT THÚC THỰC TẾ
  const [timeLeft, setTimeLeft] = useState(300);
  const expiryTimeRef = useRef(null);

  useEffect(() => {
    let interval = null;
    if (step === 2) {
      // Thiết lập mốc thời gian hết hạn (hiện tại + 5 phút)
      if (!expiryTimeRef.current) {
        expiryTimeRef.current = new Date().getTime() + 300 * 1000;
      }

      interval = setInterval(() => {
        const now = new Date().getTime();
        const distance = expiryTimeRef.current - now;

        if (distance <= 0) {
          clearInterval(interval);
          setTimeLeft(0);
        } else {
          setTimeLeft(Math.floor(distance / 1000));
        }
      }, 1000);
    } else {
      expiryTimeRef.current = null; // Reset mốc khi thoát khỏi step 2
    }
    return () => clearInterval(interval);
  }, [step]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  // Gửi Email
  const handleStep1 = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', { email });
      expiryTimeRef.current = null; // Reset để set lại mốc mới ở step 2
      setTimeLeft(300);
      setStep(2);
    } catch (err) {
      alert(err.response?.data?.message || "Email không tồn tại!");
    } finally { setLoading(false); }
  };

  // Xác nhận OTP
  const handleStep2 = (e) => {
    e.preventDefault();
    if (timeLeft === 0) {
      alert("Mã OTP đã hết hạn, vui lòng gửi lại!");
      return;
    }
    setStep(3);
  };

  // Đổi mật khẩu
  const handleStep3 = async (e) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      alert("Mật khẩu xác nhận không khớp!");
      return;
    }
    try {
      await api.post('/auth/reset-password', {
        email,
        otp,
        matKhauMoi: newPassword
      });
      alert("Đổi mật khẩu thành công!");
      goToLogin();
    } catch (err) {
      alert(err.response?.data?.message || "Mã OTP không đúng hoặc đã hết hạn!");
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        {/* Nhập Email */}
        {step === 1 && (
          <form onSubmit={handleStep1}>
            <h3>Quên mật khẩu</h3>
            <div className="input-group">
              <label>Email</label>
              <input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="Nhập email của bạn" />
            </div>
            <button type="submit" className="auth-btn" disabled={loading}>
              {loading ? "Đang xử lý..." : "Tiếp theo"}
            </button>
          </form>
        )}

        {/* Nhập OTP */}
        {step === 2 && (
          <form onSubmit={handleStep2}>
            <h3>Xác thực OTP</h3>
            <div className="input-group">
              <label>Mã OTP</label>
              <input type="text" required value={otp} onChange={e => setOtp(e.target.value)} placeholder="Nhập 6 số" />
              <p className="timer-text">
                Mã hết hạn sau: <span style={{color: timeLeft < 60 ? 'red' : '#2563eb'}}>{formatTime(timeLeft)}</span>
              </p>
            </div>
            <button type="submit" className="auth-btn">Xác nhận mã</button>
            <p className="resend-text" onClick={() => setStep(1)}>Gửi lại mã mới</p>
          </form>
        )}

        {/* Mật khẩu mới */}
        {step === 3 && (
          <form onSubmit={handleStep3}>
            <h3>Mật khẩu mới</h3>
            <div className="input-group">
              <label>Mật khẩu mới</label>
              <input type="password" required value={newPassword} onChange={e => setNewPassword(e.target.value)} placeholder="Nhập mật khẩu mới" />
            </div>
            <div className="input-group">
              <label>Xác nhận mật khẩu mới</label>
              <input type="password" required value={confirmPassword} onChange={e => setConfirmPassword(e.target.value)} placeholder="Nhập lại mật khẩu" />
            </div>
            <button type="submit" className="auth-btn">Đổi mật khẩu</button>
          </form>
        )}

        <p className="auth-nav"><span onClick={goToLogin}>Quay lại đăng nhập</span></p>
      </div>
    </div>
  );
};

export default ForgotPassword;