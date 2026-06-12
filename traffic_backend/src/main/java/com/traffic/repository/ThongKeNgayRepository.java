package com.traffic.repository;

import com.traffic.dto.response.TrafficTimeStatsResponse;
import com.traffic.entity.ThongKeNgay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ThongKeNgayRepository extends JpaRepository<ThongKeNgay, Long> {
    @Query("SELECT new com.traffic.dto.response.TrafficTimeStatsResponse(t.ngayThongKe, CAST(t.tocDoTrungBinh AS long)) " +
            "FROM ThongKeNgay t " +
            "WHERE t.ngayThongKe BETWEEN :startDate AND :endDate " +
            "ORDER BY t.ngayThongKe ASC")
    List<TrafficTimeStatsResponse> getPrecalculatedStatsByTime(
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate
    );
}