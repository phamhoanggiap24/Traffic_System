package com.traffic.repository;

import com.traffic.common.ReportStatus;
import com.traffic.dto.response.TrafficLocationStatsResponse;
import com.traffic.dto.response.TrafficTimeStatsResponse;
import com.traffic.entity.BaoCaoSuCo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BaoCaoSuCoRepository extends JpaRepository<BaoCaoSuCo, Long> {
    // CÁC HÀM THỐNG KÊ TOÀN HỆ THỐNG
    @Query("SELECT COUNT(b) FROM BaoCaoSuCo b WHERE b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI")
    long countAllVerifiedReports();

    @Query("SELECT new com.traffic.dto.response.TrafficTimeStatsResponse(" +
            "FUNCTION('DATE', b.thoiGianBaoCao), " +
            "CAST(COUNT(b) AS long)) " +
            "FROM BaoCaoSuCo b " +
            "WHERE (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND b.thoiGianBaoCao BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', b.thoiGianBaoCao) " +
            "ORDER BY FUNCTION('DATE', b.thoiGianBaoCao) ASC")
    List<TrafficTimeStatsResponse> getTrafficStatsByTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT new com.traffic.dto.response.TrafficLocationStatsResponse(b.loaiSuCo.tenLoai, COUNT(b), COUNT(b)) " +
            "FROM BaoCaoSuCo b " +
            "WHERE (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND b.thoiGianBaoCao BETWEEN :startDate AND :endDate " +
            "GROUP BY b.loaiSuCo.tenLoai " +
            "ORDER BY COUNT(b) DESC")
    List<TrafficLocationStatsResponse> getTrafficStatsByLocation(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM BaoCaoSuCo b WHERE (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND (:startDate IS NULL OR b.thoiGianBaoCao >= :startDate) " +
            "AND (:endDate IS NULL OR b.thoiGianBaoCao <= :endDate)")
    long countAllVerifiedReportsByTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // BỘ LỌC DANH SÁCH QUẢN LÝ
    @Query(value = "SELECT b FROM BaoCaoSuCo b WHERE " +
            "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
            "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
            "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
            "(b.trangThai != com.traffic.common.ReportStatus.DA_XOA) AND " +
            "(:trangThai IS NULL OR b.trangThai = :trangThai)",
            countQuery = "SELECT COUNT(b) FROM BaoCaoSuCo b WHERE " +
                    "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
                    "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
                    "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
                    "(b.trangThai != com.traffic.common.ReportStatus.DA_XOA) AND " +
                    "(:trangThai IS NULL OR b.trangThai = :trangThai)")
    Page<BaoCaoSuCo> findWithFilters(
            @Param("loaiSuCoId") Integer loaiSuCoId,
            @Param("tenDangNhap") String tenDangNhap,
            @Param("trangThai") ReportStatus trangThai,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // QUÁ HẠN (Thời gian báo cáo NHỎ HƠN mốc giới hạn)
    @Query(value = "SELECT b FROM BaoCaoSuCo b WHERE " +
            "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
            "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
            "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
            "b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN AND (" +
            "  (b.loaiSuCo.loaiSuCoId IN (1, 2) AND b.thoiGianBaoCao < :timeLimit30) OR " +
            "  (b.loaiSuCo.loaiSuCoId IN (3, 4) AND b.thoiGianBaoCao < :timeLimit60)" +
            ")")
    Page<BaoCaoSuCo> findExpiredReports(
            @Param("loaiSuCoId") Integer loaiSuCoId,
            @Param("tenDangNhap") String tenDangNhap,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("timeLimit30") LocalDateTime timeLimit30,
            @Param("timeLimit60") LocalDateTime timeLimit60,
            Pageable pageable
    );

    @Query(value = "SELECT b FROM BaoCaoSuCo b WHERE " +
            "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
            "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
            "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
            "b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN AND (" +
            "  (b.loaiSuCo.loaiSuCoId IN (1, 2) AND b.thoiGianBaoCao >= :timeLimit30) OR " +
            "  (b.loaiSuCo.loaiSuCoId IN (3, 4) AND b.thoiGianBaoCao >= :timeLimit60)" +
            ")")
    Page<BaoCaoSuCo> findActiveSuspectReports(
            @Param("loaiSuCoId") Integer loaiSuCoId,
            @Param("tenDangNhap") String tenDangNhap,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("timeLimit30") LocalDateTime timeLimit30,
            @Param("timeLimit60") LocalDateTime timeLimit60,
            Pageable pageable
    );

    // NGHI VẤN CHƯA QUÁ HẠN
    @Query(value = "SELECT COUNT(*) FROM bao_cao_su_co b WHERE b.trang_thai != 'DA_XOA' AND (" +
            "  (b.trang_thai = 'CHO_XAC_MINH' AND NOT (" +
            "    (b.loai_su_co_id IN (1, 2) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 30) OR " +
            "    (b.loai_su_co_id IN (3, 4) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 60)" +
            "  )) OR " +
            "  (b.trang_thai = 'NGHI_VAN' AND NOT (" +
            "    (b.loai_su_co_id IN (1, 2) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 30) OR " +
            "    (b.loai_su_co_id IN (3, 4) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 60)" +
            "  ))" +
            ")", nativeQuery = true)
    long countPendingReports(@Param("now") LocalDateTime now);

    // CÁC PHƯƠNG THỨC LỌC BÁN KÍNH TRÊN BẢN ĐỒ
    @Query("SELECT b FROM BaoCaoSuCo b WHERE b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH")
    List<BaoCaoSuCo> findActiveReportsForMap(
            @Param("threeHoursAgo") LocalDateTime threeHoursAgo,
            @Param("oneDayAgo") LocalDateTime oneDayAgo
    );

    @Query(value = "SELECT DATE(b.thoi_gian_bao_cao) as report_date, COUNT(DISTINCT b.bao_cao_id) as count " +
            "FROM bao_cao_su_co b " +
            "WHERE b.trang_thai != 'DA_XOA' " +
            "AND b.vi_do IS NOT NULL AND b.kinh_do IS NOT NULL " +
            "AND (b.trang_thai = 'DA_XAC_MINH' OR b.trang_thai = 'AN_HIEN_THI') " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.vi_do)) * cos(radians(b.kinh_do - :lng)) + sin(radians(:lat)) * sin(radians(b.vi_do)))) <= :radius " +
            "AND (:startDate IS NULL OR b.thoi_gian_bao_cao >= :startDate) " +
            "AND (:endDate IS NULL OR b.thoi_gian_bao_cao <= :endDate) " +
            "GROUP BY DATE(b.thoi_gian_bao_cao) " +
            "ORDER BY report_date ASC", nativeQuery = true)
    List<Object[]> getTrendWithinRadiusNative(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT l.ten_loai, COUNT(DISTINCT b.bao_cao_id) as cnt1, COUNT(DISTINCT b.bao_cao_id) as cnt2 " +
            "FROM bao_cao_su_co b " +
            "INNER JOIN loai_su_co l ON b.loai_su_co_id = l.loai_su_co_id " +
            "WHERE b.trang_thai != 'DA_XOA' " +
            "AND b.vi_do IS NOT NULL AND b.kinh_do IS NOT NULL " +
            "AND (b.trang_thai = 'DA_XAC_MINH' OR b.trang_thai = 'AN_HIEN_THI') " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.vi_do)) * cos(radians(b.kinh_do - :lng)) + sin(radians(:lat)) * sin(radians(b.vi_do)))) <= :radius " +
            "AND b.thoi_gian_bao_cao BETWEEN :startDate AND :endDate " +
            "GROUP BY l.ten_loai", nativeQuery = true)
    List<Object[]> getLocationDensityWithinRadiusNative(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(DISTINCT b.bao_cao_id) " +
            "FROM bao_cao_su_co b " +
            "WHERE b.trang_thai != 'DA_XOA' " +
            "AND b.vi_do IS NOT NULL AND b.kinh_do IS NOT NULL " +
            "AND (b.trang_thai = 'DA_XAC_MINH' OR b.trang_thai = 'AN_HIEN_THI') " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.vi_do)) * cos(radians(b.kinh_do - :lng)) + sin(radians(:lat)) * sin(radians(b.vi_do)))) <= :radius " +
            "AND b.thoi_gian_bao_cao BETWEEN :startDate AND :endDate", nativeQuery = true)
    long getOverviewWithinRadiusNative(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BaoCaoSuCo b SET b.trangThai = com.traffic.common.ReportStatus.DA_XOA WHERE b.baoCaoId = :id")
    void deleteReportSoft(@Param("id") Long id);

    // REFACTOR TO NATIVE: Khắc phục lỗi phân tích cú pháp cho Scheduler tự động duyệt bài overdue
    @Query(value = "SELECT b.* FROM bao_cao_su_co b WHERE b.trang_thai = 'CHO_XAC_MINH' AND (" +
            "  (b.loai_su_co_id IN (1, 2) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 30) OR " +
            "  (b.loai_su_co_id IN (3, 4) AND TIMESTAMPDIFF(MINUTE, b.thoi_gian_bao_cao, :now) > 60)" +
            ")", nativeQuery = true)
    List<BaoCaoSuCo> findPendingReportsOverdue(@Param("now") LocalDateTime now);

    @Query(value = "SELECT b.* FROM bao_cao_su_co b WHERE " +
            "b.loai_su_co_id = :loaiId AND " +
            "b.trang_thai = 'DA_XAC_MINH' AND " +
            "(6371000.0 * acos(cos(radians(:lat)) * cos(radians(b.vi_do)) * cos(radians(b.kinh_do - :lng)) + sin(radians(:lat)) * sin(radians(b.vi_do)))) <= :radius", nativeQuery = true)
    List<BaoCaoSuCo> findActiveNearby(
            @Param("loaiId") Object loaiId,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);
}