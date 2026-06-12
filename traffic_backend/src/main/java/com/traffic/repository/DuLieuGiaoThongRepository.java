package com.traffic.repository;

import com.traffic.dto.response.TrafficSpeedAnalyticsResponse;
import com.traffic.entity.DuLieuGiaoThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DuLieuGiaoThongRepository extends JpaRepository<DuLieuGiaoThong, Long> {

    List<DuLieuGiaoThong> findByKhuVucKhuVucId(Integer khuVucId);

    // Phân tích diễn biến vận tốc trung bình theo khung giờ tại một Khu vực cụ thể
    @Query("SELECT new com.traffic.dto.response.TrafficSpeedAnalyticsResponse(FUNCTION('HOUR', d.thoiDiemGhiNhan), AVG(d.tocDoTrungBinh)) " +
            "FROM DuLieuGiaoThong d " +
            "WHERE (:khuVucId IS NULL OR d.khuVuc.khuVucId = :khuVucId) " +
            "AND d.thoiDiemGhiNhan BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', d.thoiDiemGhiNhan) " +
            "ORDER BY FUNCTION('HOUR', d.thoiDiemGhiNhan) ASC")
    List<TrafficSpeedAnalyticsResponse> getAverageSpeedByHour(
            @Param("khuVucId") Integer khuVucId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tính vận tốc trung bình theo khung giờ trong bán kính
    @Query("SELECT new com.traffic.dto.response.TrafficSpeedAnalyticsResponse(FUNCTION('HOUR', d.thoiDiemGhiNhan), AVG(d.tocDoTrungBinh)) " +
            "FROM DuLieuGiaoThong d " +
            "WHERE d.khuVuc.viDo IS NOT NULL AND d.khuVuc.kinhDo IS NOT NULL " +
            "AND (6371 * 1000 * acos(cos(radians(:lat)) * cos(radians(d.khuVuc.viDo)) * cos(radians(d.khuVuc.kinhDo - :lng)) + sin(radians(:lat)) * sin(radians(d.khuVuc.viDo)))) <= :radius " +
            "AND d.thoiDiemGhiNhan BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', d.thoiDiemGhiNhan) " +
            "ORDER BY FUNCTION('HOUR', d.thoiDiemGhiNhan) ASC")
    List<TrafficSpeedAnalyticsResponse> getAverageSpeedByHourWithinRadius(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radius,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tính vận tốc trung bình theo khung giờ của toàn hệ thống
    @Query("SELECT new com.traffic.dto.response.TrafficSpeedAnalyticsResponse(FUNCTION('HOUR', d.thoiDiemGhiNhan), AVG(d.tocDoTrungBinh)) " +
            "FROM DuLieuGiaoThong d " +
            "WHERE d.thoiDiemGhiNhan BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('HOUR', d.thoiDiemGhiNhan) " +
            "ORDER BY FUNCTION('HOUR', d.thoiDiemGhiNhan) ASC")
    List<TrafficSpeedAnalyticsResponse> getGlobalAverageSpeedByHour(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Tính toán vận tốc trung bình của toàn bộ mạng lưới giao thông hệ thống
    @Query("SELECT COALESCE(AVG(d.tocDoTrungBinh), 0.0) " +
            "FROM DuLieuGiaoThong d " +
            "WHERE d.thoiDiemGhiNhan >= :startOfDay")
    double getAverageSystemSpeed(@Param("startOfDay") LocalDateTime startOfDay);

    // Đếm số lượng điểm nút giao thông đang bị kẹt xe nghiêm trọng
    @Query("SELECT COUNT(DISTINCT d.khuVuc.khuVucId) " +
            "FROM DuLieuGiaoThong d " +
            "WHERE d.thoiDiemGhiNhan >= :startOfDay " +
            "AND d.mucDoUnTac > 3")
    long countSevereCongestionHotspots(@Param("startOfDay") LocalDateTime startOfDay);
}