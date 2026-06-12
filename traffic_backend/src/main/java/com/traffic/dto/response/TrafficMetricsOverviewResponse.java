package com.traffic.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class TrafficMetricsOverviewResponse {
    private long totalIncidentsToday;
    private long pendingVerificationCount;
    private long monitoredAreasCount;
    private String lastSyncTime;

    // Gán dữ liệu từ Service khi xem Toàn thành phố
    public TrafficMetricsOverviewResponse(long totalIncidentsToday, long pendingVerificationCount, long monitoredAreasCount, String lastSyncTime) {
        this.totalIncidentsToday = totalIncidentsToday;
        this.pendingVerificationCount = 0L;
        this.monitoredAreasCount = monitoredAreasCount;
        this.lastSyncTime = lastSyncTime;
    }

    // Hứng dữ liệu trực tiếp từ JPQL Query của BaoCaoSuCoRepository khi quét bán kính
    public TrafficMetricsOverviewResponse(long totalIncidentsToday, long pendingVerificationCount, double averageSystemSpeed, long severeCongestionHotspots) {
        this.totalIncidentsToday = totalIncidentsToday;
        this.pendingVerificationCount = 0L;

        // Tự động ánh xạ logic sang cấu trúc giao diện mới khi người dùng quét theo bán kính
        this.monitoredAreasCount = 1L;
        this.lastSyncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")); // Đồng bộ mốc thời gian thực tại lúc quét bản đồ
    }
}