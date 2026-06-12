import React, { useState, useEffect, useCallback, useRef } from 'react';
import api from '../../../api/axiosConfig';
import { Trash2, ShieldCheck, User, AlertCircle, RefreshCw, Search, Lock, Unlock } from 'lucide-react';
import './UserManagement.css';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchUsername, setSearchUsername] = useState("");
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const isFirstRender = useRef(true);

  // Gọi API
  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('accessToken');
      const queryParams = {
        page: currentPage,
        size: 7
      };

      if (searchUsername && searchUsername.trim() !== "") {
        queryParams.tenDangNhap = searchUsername.trim();
      }

      const res = await api.get('/admin/users', {
        params: queryParams,
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.data && res.data.content) {
        setUsers(res.data.content);
        setTotalPages(res.data.totalPages);
      } else {
        setUsers(res.data || []);
        setTotalPages(1);
      }
      setError(null);
    } catch (err) {
      console.error("Lỗi:", err.response);
      setError(err.response?.status === 403
        ? "Bạn không có quyền truy cập quản trị."
        : "Không thể kết nối tới máy chủ.");
    } finally {
      setLoading(false);
    }
  }, [currentPage, searchUsername]);

  // Debounce logic để lọc tên đăng nhập
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    const delayDebounceFn = setTimeout(() => {
      setCurrentPage(0);
    }, 500);
    return () => clearTimeout(delayDebounceFn);
  }, [searchUsername]);

  // Chạy lại fetch khi trang hoặc từ khóa thay đổi
  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const handleDeleteUser = async (id) => {
    if (!window.confirm("Xác nhận xóa tài khoản?")) return;
    try {
      await api.delete(`/admin/users/${id}`);
      alert("Xóa thành công!");
      fetchUsers();
    } catch (err) {
      alert("Lỗi khi xóa tài khoản!");
    }
  };

  // Thao tác khóa tài khoản từ Admin
  const handleLockUser = async (id) => {
    if (!window.confirm("Xác nhận khóa tài khoản?")) return;
    try {
      await api.patch(`/admin/users/lock/${id}`);
      alert("Đã khóa tài khoản thành công!");
      fetchUsers();
    } catch (err) {
      alert("Lỗi khi khóa tài khoản!");
    }
  };

  // Thao tác khôi phục tài khoảntừ Admin
  const handleUnlockUser = async (id) => {
    if (!window.confirm("Xác nhận khôi phục tài khoản?")) return;
    try {
      await api.patch(`/admin/users/unlock/${id}`);
      alert("Đã khôi phục tài khoản thành công! Điểm tin cậy đặt về mức 10.");
      fetchUsers();
    } catch (err) {
      alert("Lỗi khi khôi phục tài khoản!");
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
    <div className="user-management-wrapper">
      <div className="user-management-card">
        <div className="header-flex">
          <div className="user-header-title">
            <h3>Quản lý tài khoản người dùng</h3>
            <p>
              Xem thông tin và thực hiện các thao tác quản trị tài khoản hệ thống
            </p>
          </div>
          <div className="header-actions">
            <div className="search-wrapper">
              <Search size={16} color="#64748b" />
              <input
                type="text"
                placeholder="Tìm tên đăng nhập..."
                value={searchUsername}
                onChange={(e) => setSearchUsername(e.target.value)}
              />
            </div>
            <button className="btn-refresh" onClick={fetchUsers} disabled={loading}>
              <RefreshCw size={16} className={loading ? "animate-spin" : ""} /> Làm mới
            </button>
          </div>
        </div>

        {error ? (
          <div className="error-box-custom">
            <AlertCircle size={20} />
            <span>{error}</span>
          </div>
        ) : (
          <>
            <div className={`table-responsive ${loading ? "opacity-50" : ""}`}>
              <table className="user-table">
                <thead>
                  <tr>
                    <th style={{ width: '150px', textAlign: 'center' }}>Tên đăng nhập</th>
                    <th style={{ width: '180px', textAlign: 'center' }}>Họ tên</th>
                    <th style={{ textAlign: 'center' }}>Email</th>
                    <th style={{ width: '140px', textAlign: 'center' }}>Số điện thoại</th>
                    <th style={{ width: '120px', textAlign: 'center' }}>Độ tin cậy</th>
                    <th style={{ width: '140px', textAlign: 'center' }}>Vai trò</th>
                    <th style={{ width: '120px', textAlign: 'center' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const isAdminUser = u.vaiTro?.some(r => r.includes('ADMIN'));
                    const isLocked = u.trangThaiTaiKhoan === 'LOCKED' || (u.doTinCayNguoiDung !== null && u.doTinCayNguoiDung < 5 && !isAdminUser);

                    return (
                      <tr key={u.taiKhoanId} className={isLocked ? "row-user-locked" : ""}>
                        <td style={{ textAlign: 'left', fontWeight: '600', color: isLocked ? '#94a3b8' : '#1e293b' }}>
                          {u.tenDangNhap}
                        </td>
                        <td style={{ textAlign: 'left' }}>{u.hoTen}</td>
                        <td style={{ textAlign: 'left' }}>{u.email}</td>
                        <td style={{ textAlign: 'center' }}>{u.soDienThoai || <span style={{ color: '#cbd5e1' }}>-</span>}</td>
                        <td style={{ verticalAlign: 'middle', textAlign: 'center' }}>
                          <span className="reliability-cell">
                            {isAdminUser ? (
                              <><span className="reliability-number" style={{ color: '#94a3b8' }}>—</span><span className="reliability-max">/0</span></>
                            ) : (
                              <><span className={`reliability-number ${u.doTinCayNguoiDung >= 40 ? 'reliability-high' : u.doTinCayNguoiDung >= 20 ? 'reliability-medium' : 'reliability-low'}`}>{u.doTinCayNguoiDung ?? 50}</span><span className="reliability-max">/50</span></>
                            )}
                          </span>
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          {isAdminUser ? (
                            <span className="status-badge active"><ShieldCheck size={14} /> Admin</span>
                          ) : isLocked ? (
                            <span className="status-badge locked"><Lock size={12} /> Bị khóa</span>
                          ) : (
                            <span className="status-badge"><User size={14} /> Thành viên</span>
                          )}
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <div className="action-buttons-group" style={{ display: 'flex', justifyContent: 'center', gap: '8px' }}>
                            {!isAdminUser && (
                              isLocked ? (
                                <button
                                  className="btn-action-user btn-unlock"
                                  title="Khôi phục tài khoản"
                                  onClick={() => handleUnlockUser(u.taiKhoanId)}
                                >
                                  <Unlock size={16} />
                                </button>
                              ) : (
                                <button
                                  className="btn-action-user btn-lock"
                                  title="Khóa tài khoản"
                                  onClick={() => handleLockUser(u.taiKhoanId)}
                                >
                                  <Lock size={16} />
                                </button>
                              )
                            )}
                            <button
                              className="btn-action-user btn-delete"
                              title="Xóa tài khoản"
                              onClick={() => handleDeleteUser(u.taiKhoanId)}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              {users.length === 0 && !loading && <p className="empty-text" style={{ textAlign: 'center', padding: '30px', color: '#64748b', fontStyle: 'italic' }}>Không tìm thấy tài khoản người dùng nào phù hợp.</p>}
            </div>

            <div className="pagination-wrapper">
              <button className="pagi-btn" disabled={currentPage === 0 || loading} onClick={() => setCurrentPage(p => p - 1)}>Trước</button>
              <div className="page-numbers-container">
                {renderPageNumbers().map((pageItem, index) => {
                  if (pageItem === '...') return <span key={`ellipsis-${index}`} className="page-ellipsis">...</span>;
                  return (
                    <button key={`page-${pageItem}`} className={`page-number-btn ${currentPage === pageItem ? 'active' : ''}`} disabled={loading} onClick={() => setCurrentPage(pageItem)}>{pageItem + 1}</button>
                  );
                })}
              </div>
              <button className="pagi-btn" disabled={currentPage >= totalPages - 1 || loading} onClick={() => setCurrentPage(p => p + 1)}>Sau</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default UserManagement;