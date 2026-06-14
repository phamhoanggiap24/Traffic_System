import React, { useState, useEffect, useCallback, useRef } from 'react';
import api from '../../../api/axiosConfig';
import './ReportList.css';
import { MapPin, Search, Trash2, AlertTriangle, CheckCircle, RefreshCw, Clock, ChevronDown, Calendar, EyeOff, Image as ImageIcon, X } from 'lucide-react';
import { getRelativeTime } from '../../../utils/timeFormatter';

const ReportList = ({ setActiveTab }) => {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  // State để lưu ảnh khi được click phóng to
  const [modalImage, setModalImage] = useState(null);
  
  // State để trigger re-render mỗi giây để cập nhật thời gian thực
  const [, setTimeUpdate] = useState(0);

  // States cho bộ lọc
  const [searchUser, setSearchUser] = useState("");
  const [selectedLoai, setSelectedLoai] = useState("");
  const [selectedStatus, setSelectedStatus] = useState("");
  const [filterDate, setFilterDate] = useState("");

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Tự động thay đổi để vừa với màn hình của Admin
  const [dynamicPageSize, setDynamicPageSize] = useState(7);
  const containerRef = useRef(null);

  // Kiểm tra kích thước khung chứa bảng để tính toán số dòng tối đa trước khi bị tràn
  useEffect(() => {
    if (!containerRef.current) return;

    const resizeObserver = new ResizeObserver((entries) => {
      for (let entry of entries) {
        const containerHeight = entry.contentRect.height;
        const headerHeight = 45;
        const rowHeight = 49;

        const availableHeight = containerHeight - headerHeight;
        if (availableHeight > 0) {
          const calculatedSize = Math.floor(availableHeight / rowHeight);
          setDynamicPageSize(calculatedSize > 0 ? calculatedSize : 1);
        }
      }
    });
    resizeObserver.observe(containerRef.current);
    return () => resizeObserver.disconnect();
  }, []);

  // Update thời gian thực mỗi giây để hiển thị "5 phút trước", "10 phút trước" v.v.
  useEffect(() => {
    const interval = setInterval(() => {
      setTimeUpdate(prev => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  // Tính trạng thái thực tế dựa theo từng loại sự cố
  const getEffectiveStatus = useCallback((item) => {
      if (['DA_XAC_MINH', 'SAI_SU_THAT', 'AN_HIEN_THI', 'DA_XOA'].includes(item.trangThai)) {
        return item.trangThai;
      }

      // Kiểm tra thời gian hết hạn
      try {
        const reportTime = new Date(item.thoiGianBaoCao);
        const currentTime = new Date();
        const timeDiffMs = currentTime.getTime() - reportTime.getTime();

        // Xác định cấu hình thời gian hết hạn dựa trên loaiSuCoId (chính xác hơn)
        let expireHours = 24;
        // loaiSuCoId: 1 = Ùn tắc, 2 = Tai nạn -> 3 giờ
        // loaiSuCoId: 3 = Ngập lụt, 4 = Công trình -> 24 giờ
        if (item.loaiSuCoId === 1 || item.loaiSuCoId === 2) {
          expireHours = 3;
        }
        const expireLimit = expireHours * 60 * 60 * 1000;

        if ((item.trangThai === 'CHO_XAC_MINH' || item.trangThai === 'NGHI_VAN') && timeDiffMs > expireLimit) {
          return 'QUA_HAN';
        }
      } catch (err) {
        console.error('Lỗi khi tính thời gian hết hạn:', err, item);
      }
      
      return item.trangThai;
    }, []);

  // Tải dữ liệu API từ Backend
  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const queryParams = {
        page: currentPage,
        size: dynamicPageSize
      };

      if (searchUser && searchUser.trim() !== "") queryParams.tenDangNhap = searchUser.trim();
      if (selectedLoai) queryParams.loaiSuCoId = selectedLoai;
      if (selectedStatus) queryParams.trangThai = selectedStatus;
      if (filterDate) queryParams.ngay = filterDate;

      const res = await api.get('/report/admin/danh-sach', { params: queryParams });

      const responseData = res.data?.data || res.data;
      if (responseData && responseData.content) {
        setReports(responseData.content);
        setTotalPages(responseData.totalPages);
      } else {
        setReports([]);
      }
    } catch (err) {
      console.error("Lỗi tải danh sách:", err);
    } finally {
      setLoading(false);
    }
  }, [currentPage, dynamicPageSize, selectedLoai, searchUser, selectedStatus, filterDate]);

  // Tự động đưa về trang 0 khi thay đổi bộ lọc
  useEffect(() => {
    setCurrentPage(0);
  }, [searchUser, selectedLoai, selectedStatus, filterDate, dynamicPageSize]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  // XỬ LÝ DUYỆT / TỪ CHỐI BÁO CÁO
  const handleVerify = async (id, trangThaiMoi) => {
    const confirmMsg = trangThaiMoi === 'DA_XAC_MINH'
      ? "Xác nhận báo cáo?"
      : "Từ chối báo cáo?";
    if (!window.confirm(confirmMsg)) return;
    setIsProcessing(true);
    try {
      await api.patch(`/report/admin/xac-minh/${id}`, null, { params: { trangThaiMoi } });
      window.dispatchEvent(new Event('incident-verified'));
      await loadReports();
    } catch (err) {
      alert("Lỗi khi cập nhật trạng thái xác minh!");
    } finally {
      setIsProcessing(false);
    }
  };

  // XỬ LÝ XÓA BÁO CÁO
  const handleDelete = async (id) => {
    if (!window.confirm("Xác nhận xóa báo cáo khỏi danh sách?")) return;
    setIsProcessing(true);
    try {
      await api.delete(`/report/admin/xoa/${id}`);
      alert("Đã xóa báo cáo thành công.");
      window.dispatchEvent(new Event('incident-verified'));
      await loadReports();
    } catch (err) {
      alert("Lỗi hệ thống khi thực hiện xóa dữ liệu!");
    } finally {
      setIsProcessing(false);
    }
  };

  // ĐIỀU HƯỚNG SANG MAP
  const handleViewOnMap = (item) => {
    const effectiveStatus = getEffectiveStatus(item);
    const focusData = {
      id: item.baoCaoId || item.id,
      baoCaoId: item.baoCaoId || item.id,
      lat: item.viDo,
      lng: item.kinhDo,
      viDo: item.viDo,
      kinhDo: item.kinhDo,
      tenLoaiSuCo: item.tenLoaiSuCo,
      moTa: item.moTa,
      thoiGianBaoCao: item.thoiGianBaoCao,
      trangThai: effectiveStatus,
      hinhAnhUrl: item.hinhAnhUrl
    };
    localStorage.setItem('focusLocation', JSON.stringify(focusData));
    window.dispatchEvent(new Event('focus-accident'));

    if (typeof setActiveTab === 'function') {
      setActiveTab('map');
    } else {
      window.location.href = "/map";
    }
  };

  const renderPageNumbers = () => {
    const pages = [];
    const leftSide = Math.max(0, currentPage - 2);
    const rightSide = Math.min(totalPages - 1, currentPage + 2);

    if (leftSide > 0) {
      pages.push(0);
      if (leftSide > 1) pages.push('...');
    }

    for (let i = leftSide; i <= rightSide; i++) {
      pages.push(i);
    }

    if (rightSide < totalPages - 1) {
      if (rightSide < totalPages - 2) pages.push('...');
      pages.push(totalPages - 1);
    }

    return pages;
  };

  return (
    <div className="report-list-wrapper">
      {modalImage && (
        <div className="image-modal-overlay" onClick={() => setModalImage(null)}>
          <div className="image-modal-content" onClick={(e) => e.stopPropagation()}>
            <button className="close-modal" onClick={() => setModalImage(null)}>
              <X size={24} />
            </button>
            <img
              src={modalImage.startsWith('http') ? modalImage : `http://localhost:8080${modalImage}`}
              alt="Hình ảnh sự cố đính kèm thực tế"
              onError={(e) => {
                console.error('Lỗi tải hình ảnh:', e.target.src);
                e.target.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='600' height='400'%3E%3Crect fill='%23f0f0f0' width='600' height='400'/%3E%3Ctext x='50%25' y='50%25' font-family='Arial' font-size='20' fill='%23999' text-anchor='middle' dy='.3em'%3EKhông thể tải hình ảnh%3C/text%3E%3C/svg%3E";
              }}
            />
          </div>
        </div>
      )}

      <div className="report-management-card">
        <div className="report-header-flex">
          <div className="report-header-title">
            <h3>Danh sách báo cáo sự cố</h3>
            <p>Quản lý và phê duyệt báo cáo từ người dùng</p>
          </div>

          <div className="header-actions" style={{ display: 'flex', gap: '10px' }}>
            <div className="search-wrapper">
              <Search size={16} color="#64748b" />
              <input
                type="text"
                placeholder="Tìm tên đăng nhập..."
                value={searchUser}
                onChange={(e) => setSearchUser(e.target.value)}
              />
            </div>

            <button className="btn-refresh" onClick={loadReports} disabled={loading || isProcessing}>
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} /> Làm mới
            </button>
          </div>
        </div>

        <div ref={containerRef} className={`table-responsive ${loading ? "opacity-50" : ""}`}>
          <div className="table-scroll-container">
            <table className="report-table">
              <thead>
                <tr>
                  <th style={{ width: '160px' }}>
                    <div className="th-with-filter">
                      <span>Thời gian</span>
                      <div className="date-filter-icon">
                        <Calendar size={14} />
                        <input
                          type="date"
                          value={filterDate}
                          onChange={(e) => setFilterDate(e.target.value)}
                        />
                      </div>
                    </div>
                  </th>
                  <th style={{ width: '180px', textAlign: 'center' }}>Người báo cáo</th>
                  <th style={{ width: '140px' }}>
                    <div className="th-with-filter">
                      <span>Loại sự cố</span>
                      <div className="select-filter-wrapper">
                        <ChevronDown size={14} />
                        <select value={selectedLoai} onChange={(e) => setSelectedLoai(e.target.value)}>
                          <option value="">Tất cả</option>
                          <option value="1">Ùn tắc</option>
                          <option value="2">Tai nạn</option>
                          <option value="3">Ngập lụt</option>
                          <option value="4">Công trình</option>
                        </select>
                      </div>
                    </div>
                  </th>
                  <th>Mô tả</th>
                  <th style={{ width: '100px', textAlign: 'center' }}>Vị trí</th>
                  <th style={{ width: '80px', textAlign: 'center' }}>Ảnh</th>
                  <th style={{ width: '140px' }}>
                    <div className="th-with-filter">
                      <span>Trạng thái</span>
                      <div className="select-filter-wrapper">
                        <ChevronDown size={14} />
                        <select value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)}>
                          <option value="">Tất cả</option>
                          <option value="CHO_XAC_MINH">Chờ duyệt</option>
                          <option value="DA_XAC_MINH">Xác nhận</option>
                          <option value="SAI_SU_THAT">Tin giả</option>
                          <option value="NGHI_VAN">Nghi vấn</option>
                          <option value="QUA_HAN">Quá hạn</option>
                          <option value="AN_HIEN_THI">Đã gỡ khỏi Map</option>
                        </select>
                      </div>
                    </div>
                  </th>
                  <th style={{ width: '160px', textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((item) => {
                  const effectiveStatus = getEffectiveStatus(item);
                  return (
                    <tr key={item.baoCaoId || item.id}>
                      <td>{getRelativeTime(item.thoiGianBaoCao)}</td>
                      <td style={{ fontWeight: '600', color: '#1e293b' }}>{item.tenDangNhap}</td>
                      <td style={{ textAlign: 'center' }}>
                        <span className="incident-type">{item.tenLoaiSuCo}</span>
                      </td>
                      <td className="col-mota">
                        <div className="cell-mota-truncate">{item.moTa}</div>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <button className="btn-view-map" onClick={() => handleViewOnMap(item)}>
                          <MapPin size={14} /> Xem
                        </button>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {item.hinhAnhUrl ? (
                          <button
                            className="btn-view-image"
                            onClick={() => setModalImage(item.hinhAnhUrl)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer' }}
                          >
                            <ImageIcon size={16} color="#3b82f6" />
                          </button>
                        ) : (
                          <span style={{ color: '#cbd5e1' }}>-</span>
                        )}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <span className={`status-tag status-${effectiveStatus}`}>
                          {effectiveStatus === 'SAI_SU_THAT' && <AlertTriangle size={12} />}
                          {effectiveStatus === 'DA_XAC_MINH' && <CheckCircle size={12} />}
                          {effectiveStatus === 'QUA_HAN' && <Clock size={12} />}
                          {effectiveStatus === 'AN_HIEN_THI' && <EyeOff size={12} />}
                          {effectiveStatus === 'DA_XOA' && <Trash2 size={12} />}

                          {/* Thêm điều kiện render text cho nhãn DA_XOA */}
                          {effectiveStatus === 'SAI_SU_THAT' ? 'Tin giả' :
                            effectiveStatus === 'DA_XAC_MINH' ? 'Xác nhận' :
                            effectiveStatus === 'QUA_HAN' ? 'Quá hạn' :
                            effectiveStatus === 'NGHI_VAN' ? 'Nghi vấn' :
                            effectiveStatus === 'AN_HIEN_THI' ? 'Đã gỡ khỏi Map' :
                            effectiveStatus === 'DA_XOA' ? 'Đã xóa' : 'Chờ duyệt'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <div className="actions" style={{ justifyContent: 'center', gap: '6px' }}>
                          {effectiveStatus === 'CHO_XAC_MINH' || effectiveStatus === 'NGHI_VAN' ? (
                            <>
                              <button className="btn-approve" style={{ padding: '4px 10px', background: '#10b981', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: '600' }} disabled={isProcessing} onClick={() => handleVerify(item.baoCaoId || item.id, 'DA_XAC_MINH')}>Duyệt</button>
                              <button className="btn-reject" style={{ padding: '4px 10px', background: '#ef4444', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '12px', fontWeight: '600' }} disabled={isProcessing} onClick={() => handleVerify(item.baoCaoId || item.id, 'SAI_SU_THAT')}>Từ chối</button>
                            </>
                          ) : (
                            // Nếu báo cáo đã duyệt, từ chối hoặc đã gỡ khỏi map, nút xóa sẽ xuất hiện
                            effectiveStatus !== 'DA_XOA' && (
                              <button className="btn-delete" disabled={isProcessing} onClick={() => handleDelete(item.baoCaoId || item.id)}>
                                <Trash2 size={18} />
                              </button>
                            )
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          {reports.length === 0 && !loading && <div className="empty-row">Không tìm thấy báo cáo nào.</div>}
        </div>

        <div className="pagination-wrapper">
          <button
            className="pagi-btn"
            disabled={currentPage === 0 || loading || isProcessing}
            onClick={() => setCurrentPage(p => p - 1)}
          >
            Trước
          </button>

          <div className="page-numbers-container">
            {renderPageNumbers().map((pageItem, index) => {
              if (pageItem === '...') {
                return <span key={`ellipsis-${index}`} className="page-ellipsis">...</span>;
              }
              return (
                <button
                  key={`page-${pageItem}`}
                  className={`page-number-btn ${currentPage === pageItem ? 'active' : ''}`}
                  disabled={loading || isProcessing}
                  onClick={() => setCurrentPage(pageItem)}
                >
                  {pageItem + 1}
                </button>
              );
            })}
          </div>

          <button
            className="pagi-btn"
            disabled={currentPage >= totalPages - 1 || loading || isProcessing}
            onClick={() => setCurrentPage(p => p + 1)}
          >
            Sau
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReportList;