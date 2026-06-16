import React, { useState } from 'react';
import { Send, X, Image, XCircle } from 'lucide-react';
import './ReportForm.css';
import api from '../../../api/axiosConfig';

const ReportForm = ({ selectedPoint, onClose }) => {
  const [report, setReport] = useState({
    loaiSuCo: 'Ùn tắc',
    moTa: '',
  });

  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleImageChange = (e) => {
    const file = e.target.files?.[0];

    if (!file) return;

    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

    if (!allowedTypes.includes(file.type)) {
      alert('Vui lòng chọn ảnh JPG, JPEG, PNG hoặc WEBP. Ảnh HEIC/HEIF từ điện thoại chưa được hỗ trợ.');
      e.target.value = '';
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      alert('Dung lượng ảnh tối đa là 10MB!');
      e.target.value = '';
      return;
    }

    if (imagePreview) {
      URL.revokeObjectURL(imagePreview);
    }

    setImageFile(file);
    setImagePreview(URL.createObjectURL(file));
  };

  const handleRemoveImage = () => {
    setImageFile(null);

    if (imagePreview) {
      URL.revokeObjectURL(imagePreview);
      setImagePreview(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (isSubmitting) return;

    const mapLoaiSuCo = {
      'Ùn tắc': 1,
      'Tai nạn': 2,
      'Ngập lụt': 3,
      'Công trình': 4
    };

    setIsSubmitting(true);

    try {
      const userJson = localStorage.getItem('user');
      const currentUser = userJson ? JSON.parse(userJson) : null;

      const formData = new FormData();

      formData.append('moTa', report.moTa);
      formData.append('viDo', selectedPoint.lat);
      formData.append('kinhDo', selectedPoint.lng);
      formData.append('loaiSuCoId', mapLoaiSuCo[report.loaiSuCo]);

      if (currentUser?.taiKhoanId) {
        formData.append('taiKhoanId', currentUser.taiKhoanId);
      }

      if (imageFile) {
        formData.append('hinhAnh', imageFile, imageFile.name);
      }

      const response = await api.post('/report/gui', formData);

      if (response.status === 200 || response.status === 201) {
        window.dispatchEvent(new Event('incident-verified'));
        window.dispatchEvent(new Event('report-created'));

        alert('Gửi báo cáo thành công!');
        onClose();
      }
    } catch (err) {
      console.error('Lỗi chi tiết:', err.response?.data || err.message);
      alert('Lỗi: ' + (err.response?.data?.message || 'Không thể gửi báo cáo'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="report-modal-overlay">
      <div className="report-modal-card">
        <div className="report-header">
          <h3>Báo cáo sự cố giao thông</h3>
          <button className="close-btn" onClick={onClose} type="button">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Vị trí sự cố</label>
            <div className="static-location-display">
              {selectedPoint?.address || 'Chưa xác định vị trí'}
            </div>
          </div>

          <div className="form-group">
            <label>Loại sự cố</label>
            <select
              value={report.loaiSuCo}
              onChange={(e) => setReport({ ...report, loaiSuCo: e.target.value })}
            >
              <option value="Ùn tắc">Ùn tắc</option>
              <option value="Tai nạn">Tai nạn</option>
              <option value="Ngập lụt">Ngập lụt</option>
              <option value="Công trình">Công trình</option>
            </select>
          </div>

          <div className="form-group">
            <label>Mô tả chi tiết</label>
            <textarea
              rows="4"
              placeholder="Nhập thêm chi tiết về tình trạng tại hiện trường (tối đa 200 ký tự)..."
              value={report.moTa}
              onChange={(e) => setReport({ ...report, moTa: e.target.value })}
              maxLength={200}
              required
            />
            <small style={{ display: 'block', textAlign: 'right', color: '#64748b', marginTop: '4px' }}>
              {report.moTa.length}/200 ký tự
            </small>
          </div>

          <div className="form-group">
            <label>Hình ảnh hiện trường (Không bắt buộc)</label>

            {!imagePreview ? (
              <label className="image-upload-dropzone">
                <Image size={24} color="#64748b" />
                <span>Bấm để tải ảnh lên</span>
                <input
                  type="file"
                  accept="image/jpeg,image/jpg,image/png,image/webp"
                  onChange={handleImageChange}
                  style={{ display: 'none' }}
                />
              </label>
            ) : (
              <div className="image-preview-container">
                <img src={imagePreview} alt="Preview hiện trường" className="image-preview-img" />
                <button type="button" className="remove-image-btn" onClick={handleRemoveImage}>
                  <XCircle size={18} />
                </button>
              </div>
            )}
          </div>

          <button type="submit" className="submit-report-btn" disabled={isSubmitting}>
            <Send size={18} />
            <span>{isSubmitting ? 'Đang gửi...' : 'Gửi báo cáo ngay'}</span>
          </button>
        </form>
      </div>
    </div>
  );
};

export default ReportForm;