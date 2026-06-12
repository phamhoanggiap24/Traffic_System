package com.traffic.service.impl;

import com.traffic.dto.request.AnalyticsFilterRequest;
import com.traffic.dto.response.*;
import com.traffic.entity.KhuVuc;
import com.traffic.repository.BaoCaoSuCoRepository;
import com.traffic.repository.DuLieuGiaoThongRepository;
import com.traffic.repository.KhuVucRepository;
import com.traffic.service.TrafficAnalyticsService;
import com.traffic.service.TrafficIntegrationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class TrafficAnalyticsServiceImpl implements TrafficAnalyticsService {

    @Autowired
    private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @Autowired
    private DuLieuGiaoThongRepository duLieuGiaoThongRepository;

    @Autowired
    private KhuVucRepository khuVucRepository;

    @Autowired
    private TrafficIntegrationService trafficIntegrationService;

    @Override
    @Transactional
    public TrafficMetricsOverviewResponse getOverviewMetrics(AnalyticsFilterRequest filter) {
        // Lấy mốc thời gian lọc
        LocalDateTime start = filter.getStartDate() != null ? filter.getStartDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = filter.getEndDate() != null ? filter.getEndDate() : LocalDateTime.now();
        String currentSyncTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        long totalMonitoredAreas = khuVucRepository.count();

        // Người dùng chọn một vùng cụ thể trên bản đồ
        if (filter.getLat() != null && filter.getLng() != null) {

            long actualCamerasInRadius = khuVucRepository.countCamerasWithinRadius(
                    filter.getLat(), filter.getLng(), filter.getRadius()
            );

            // Tự động tạo vùng nếu bấm vào điểm mới
            if (actualCamerasInRadius == 0) {
                log.info(">>> Phát hiện điểm click mới [{}, {}] chưa có trạm giám sát. Tiến hành tự động khởi tạo...", filter.getLat(), filter.getLng());
                try {
                    KhuVuc kvMoi = new KhuVuc();
                    String latStr = filter.getLat().toString();
                    String formattedName = "Trạm tự động quét - " + (latStr.length() > 6 ? latStr.substring(0, 6) : latStr);
                    kvMoi.setTenKhuVuc(formattedName);
                    kvMoi.setViDo(filter.getLat());
                    kvMoi.setKinhDo(filter.getLng());

                    kvMoi = khuVucRepository.save(kvMoi);

                    if (kvMoi.getKhuVucId() != null) {
                        trafficIntegrationService.updateTrafficData(filter.getLat(), filter.getLng(), kvMoi.getKhuVucId().intValue());
                    }
                } catch (Exception e) {
                    log.error("Lỗi khi tự động khởi tạo vùng: {}", e.getMessage());
                }
                actualCamerasInRadius = 1;
            }

            // Lấy dữ liệu đếm số lượng sự cố nằm trong bán kính
            TrafficMetricsOverviewResponse radiusOverview = baoCaoSuCoRepository.getOverviewWithinRadius(
                    filter.getLat(), filter.getLng(), filter.getRadius(), start, end
            );

            if (radiusOverview != null) {
                return new TrafficMetricsOverviewResponse(
                        radiusOverview.getTotalIncidentsToday(),
                        0L,
                        actualCamerasInRadius,
                        currentSyncTime
                );
            }
            return new TrafficMetricsOverviewResponse(0L, 0L, actualCamerasInRadius, currentSyncTime);
        }

        // Người dùng xem toàn thành phố
        long totalVerified = baoCaoSuCoRepository.countAllVerifiedReportsByTime(start, end);

        return new TrafficMetricsOverviewResponse(totalVerified, 0L, totalMonitoredAreas, currentSyncTime);
    }

    @Override
    public List<TrafficTimeStatsResponse> getIncidentTrendAnalysis(AnalyticsFilterRequest filter) {
        LocalDateTime start = filter.getStartDate() != null ? filter.getStartDate() : LocalDateTime.now().minusDays(7);
        LocalDateTime end = filter.getEndDate() != null ? filter.getEndDate() : LocalDateTime.now();

        if (filter.getLat() != null && filter.getLng() != null) {
            return baoCaoSuCoRepository.getTrendWithinRadius(filter.getLat(), filter.getLng(), filter.getRadius(), start, end);
        }
        return baoCaoSuCoRepository.getTrafficStatsByTime(start, end);
    }

    @Override
    public List<TrafficLocationStatsResponse> getIncidentLocationDensityAnalysis(AnalyticsFilterRequest filter) {
        LocalDateTime start = filter.getStartDate() != null ? filter.getStartDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = filter.getEndDate() != null ? filter.getEndDate() : LocalDateTime.now();

        if (filter.getLat() != null && filter.getLng() != null) {
            return baoCaoSuCoRepository.getLocationDensityWithinRadius(
                    filter.getLat(),
                    filter.getLng(),
                    filter.getRadius(),
                    start,
                    end
            );
        }

        return baoCaoSuCoRepository.getTrafficStatsByLocation(start, end);
    }

    @Override
    public List<TrafficSpeedAnalyticsResponse> getHourlySpeedEvolution(AnalyticsFilterRequest filter) {
        LocalDateTime start = filter.getStartDate() != null ? filter.getStartDate() : LocalDateTime.now().minusDays(30);
        LocalDateTime end = filter.getEndDate() != null ? filter.getEndDate() : LocalDateTime.now();

        if (filter.getLat() != null && filter.getLng() != null) {
            return duLieuGiaoThongRepository.getAverageSpeedByHourWithinRadius(
                    filter.getLat(),
                    filter.getLng(),
                    filter.getRadius(),
                    start,
                    end
            );
        }

        return duLieuGiaoThongRepository.getGlobalAverageSpeedByHour(start, end);
    }
}