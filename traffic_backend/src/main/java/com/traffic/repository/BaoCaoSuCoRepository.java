package com.traffic.repository;

import com.traffic.common.ReportStatus;
import com.traffic.dto.response.TrafficLocationStatsResponse;
import com.traffic.dto.response.TrafficMetricsOverviewResponse;
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

    // =========================================================================
    // 1. CÁC HÀM THỐNG KÊ TOÀN HỆ THỐNG
    // =========================================================================
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

    // =========================================================================
    // 2. BỘ LỌC DANH SÁCH QUẢN LÝ (SỬA LỖI TOÁN TỬ VÀ THÊM COUNT QUERY)
    // =========================================================================
    @Query(value = "SELECT b FROM BaoCaoSuCo b WHERE " +
            "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
            "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
            "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
            "(b.trangThai != com.traffic.common.ReportStatus.DA_XOA) AND " +
            "(" +
            "  (:trangThai IS NULL) OR " +
            "  (:trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND (" +
            "       ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 30) OR " +
            "       ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 60)" +
            "  )) OR " +
            "  (:trangThai = com.traffic.common.ReportStatus.NGHI_VAN AND b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) OR " +
            "  (:trangThai = com.traffic.common.ReportStatus.QUA_HAN AND (b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) AND (" +
            "       ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 30) OR " +
            "       ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 60)" +
            "  )) OR " +
            "  (:trangThai != com.traffic.common.ReportStatus.CHO_XAC_MINH AND :trangThai != com.traffic.common.ReportStatus.QUA_HAN AND b.trangThai = :trangThai)" +
            ")",
            countQuery = "SELECT COUNT(b) FROM BaoCaoSuCo b WHERE " +
                    "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
                    "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
                    "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
                    "(b.trangThai != com.traffic.common.ReportStatus.DA_XOA) AND " +
                    "(" +
                    "  (:trangThai IS NULL) OR " +
                    "  (:trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND (" +
                    "       ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 30) OR " +
                    "       ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 60)" +
                    "  )) OR " +
                    "  (:trangThai = com.traffic.common.ReportStatus.NGHI_VAN AND b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) OR " +
                    "  (:trangThai = com.traffic.common.ReportStatus.QUA_HAN AND (b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) AND (" +
                    "       ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 30) OR " +
                    "       ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 60)" +
                    "  )) OR " +
                    "  (:trangThai != com.traffic.common.ReportStatus.CHO_XAC_MINH AND :trangThai != com.traffic.common.ReportStatus.QUA_HAN AND b.trangThai = :trangThai)" +
                    ")")
    Page<BaoCaoSuCo> findWithFilters(
            @Param("loaiSuCoId") Integer loaiSuCoId,
            @Param("tenDangNhap") String tenDangNhap,
            @Param("trangThai") ReportStatus trangThai,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query(value = "SELECT b FROM BaoCaoSuCo b WHERE " +
            "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
            "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
            "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
            "(b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) AND (" +
            "  ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 30) OR " +
            "  ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 60)" +
            ")",
            countQuery = "SELECT COUNT(b) FROM BaoCaoSuCo b WHERE " +
                    "(:loaiSuCoId IS NULL OR b.loaiSuCo.loaiSuCoId = :loaiSuCoId) AND " +
                    "(:tenDangNhap IS NULL OR :tenDangNhap = '' OR b.taiKhoan.tenDangNhap LIKE CONCAT('%', :tenDangNhap, '%')) AND " +
                    "(:start IS NULL OR b.thoiGianBaoCao BETWEEN :start AND :end) AND " +
                    "(b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.NGHI_VAN) AND (" +
                    "  ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 30) OR " +
                    "  ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 60)" +
                    ")")
    Page<BaoCaoSuCo> findExpiredReports(
            @Param("loaiSuCoId") Integer loaiSuCoId,
            @Param("tenDangNhap") String tenDangNhap,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("SELECT COUNT(b) FROM BaoCaoSuCo b WHERE " +
            "b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND (" +
            "  ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 30) OR " +
            "  ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) <= 60)" +
            ")")
    long countPendingReports(@Param("now") LocalDateTime now);

    // =========================================================================
    // 3. CÁC PHƯƠNG THỨC LỌC BÁN KÍNH TRÊN BẢN ĐỒ
    // =========================================================================
    @Query("SELECT b FROM BaoCaoSuCo b WHERE " +
            "b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH AND (" +
            "  ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND b.thoiGianXacMinh >= :threeHoursAgo) OR " +
            "  ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND b.thoiGianXacMinh >= :oneDayAgo)" +
            ")")
    List<BaoCaoSuCo> findActiveReportsForMap(
            @Param("threeHoursAgo") LocalDateTime threeHoursAgo,
            @Param("oneDayAgo") LocalDateTime oneDayAgo
    );

    @Query("SELECT new com.traffic.dto.response.TrafficTimeStatsResponse(" +
            "FUNCTION('DATE', b.thoiGianBaoCao), CAST(COUNT(DISTINCT b.baoCaoId) AS long)) " +
            "FROM BaoCaoSuCo b " +
            "WHERE b.trangThai != com.traffic.common.ReportStatus.DA_XOA " +
            "AND b.viDo IS NOT NULL AND b.kinhDo IS NOT NULL " +
            "AND (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.viDo)) * cos(radians(b.kinhDo - :lng)) + sin(radians(:lat)) * sin(radians(b.viDo)))) <= :radius " +
            "AND (:startDate IS NULL OR b.thoiGianBaoCao >= :startDate) " +
            "AND (:endDate IS NULL OR b.thoiGianBaoCao <= :endDate) " +
            "GROUP BY FUNCTION('DATE', b.thoiGianBaoCao) " +
            "ORDER BY FUNCTION('DATE', b.thoiGianBaoCao) ASC")
    List<TrafficTimeStatsResponse> getTrendWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT new com.traffic.dto.response.TrafficLocationStatsResponse(b.loaiSuCo.tenLoai, CAST(COUNT(DISTINCT b.baoCaoId) AS long), CAST(COUNT(DISTINCT b.baoCaoId) AS long)) " +
            "FROM BaoCaoSuCo b " +
            "WHERE b.trangThai != com.traffic.common.ReportStatus.DA_XOA " +
            "AND b.viDo IS NOT NULL AND b.kinhDo IS NOT NULL " +
            "AND (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.viDo)) * cos(radians(b.kinhDo - :lng)) + sin(radians(:lat)) * sin(radians(b.viDo)))) <= :radius " +
            "AND b.thoiGianBaoCao BETWEEN :startDate AND :endDate " +
            "GROUP BY b.loaiSuCo.tenLoai")
    List<TrafficLocationStatsResponse> getLocationDensityWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT new com.traffic.dto.response.TrafficMetricsOverviewResponse(CAST(COUNT(DISTINCT b.baoCaoId) AS long), 0L, 0.0, 0L) " +
            "FROM BaoCaoSuCo b " +
            "WHERE b.trangThai != com.traffic.common.ReportStatus.DA_XOA " +
            "AND b.viDo IS NOT NULL AND b.kinhDo IS NOT NULL " +
            "AND (b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH OR b.trangThai = com.traffic.common.ReportStatus.AN_HIEN_THI) " +
            "AND (6371.0 * 1000.0 * acos(cos(radians(:lat)) * cos(radians(b.viDo)) * cos(radians(b.kinhDo - :lng)) + sin(radians(:lat)) * sin(radians(b.viDo)))) <= :radius " +
            "AND b.thoiGianBaoCao BETWEEN :startDate AND :endDate")
    TrafficMetricsOverviewResponse getOverviewWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE BaoCaoSuCo b SET b.trangThai = com.traffic.common.ReportStatus.DA_XOA WHERE b.baoCaoId = :id")
    void deleteReportSoft(@Param("id") Long id);

    @Query("SELECT b FROM BaoCaoSuCo b WHERE b.trangThai = com.traffic.common.ReportStatus.CHO_XAC_MINH AND (" +
            "  ((b.loaiSuCo.loaiSuCoId = 1 OR b.loaiSuCo.loaiSuCoId = 2) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 30) OR " +
            "  ((b.loaiSuCo.loaiSuCoId = 3 OR b.loaiSuCo.loaiSuCoId = 4) AND FUNCTION('TIMESTAMPDIFF', MINUTE, b.thoiGianBaoCao, :now) > 60)" +
            ")")
    List<BaoCaoSuCo> findPendingReportsOverdue(@Param("now") LocalDateTime now);

    // THAY ĐỔI: Chuyển tham số loaiId từ Integer thành Object để tương thích với cấu trúc của file Impl cũ khi truyền dữ liệu tự động.
    @Query("SELECT b FROM BaoCaoSuCo b WHERE " +
            "b.loaiSuCo.loaiSuCoId = :loaiId AND " +
            "b.trangThai = com.traffic.common.ReportStatus.DA_XAC_MINH AND " +
            "(6371000.0 * acos(cos(radians(:lat)) * cos(radians(b.viDo)) * cos(radians(b.kinhDo - :lng)) + sin(radians(:lat)) * sin(radians(b.viDo)))) <= :radius")
    List<BaoCaoSuCo> findActiveNearby(
            @Param("loaiId") Object loaiId,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius);
}