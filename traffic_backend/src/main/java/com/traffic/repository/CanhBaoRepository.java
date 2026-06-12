package com.traffic.repository;

import com.traffic.entity.BaoCaoSuCo;
import com.traffic.entity.CanhBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CanhBaoRepository extends JpaRepository<CanhBao, Long> {

    // Dành cho User
    @Query("SELECT c FROM CanhBao c WHERE c.taiKhoan.taiKhoanId = :taiKhoanId ORDER BY c.thoiGianGui DESC")
    List<CanhBao> findByTaiKhoanId(@Param("taiKhoanId") Long taiKhoanId);

    // Dành cho Admin
    @Query("SELECT c FROM CanhBao c " +
            "JOIN c.taiKhoan tk " +
            "JOIN tk.danhSachPhanQuyen pq " +
            "JOIN pq.vaiTro vt " +
            "WHERE vt.tenVaiTro = 'ROLE_ADMIN' OR vt.tenVaiTro LIKE '%ADMIN%' " +
            "ORDER BY c.thoiGianGui DESC")
    List<CanhBao> findNotificationsForAdmin();

}