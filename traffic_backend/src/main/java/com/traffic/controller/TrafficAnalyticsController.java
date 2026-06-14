package com.traffic.controller;

import com.traffic.dto.request.AnalyticsFilterRequest;
import com.traffic.dto.response.*;
import com.traffic.service.TrafficAnalyticsService;
import com.traffic.service.TrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.temporal.ChronoUnit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
public class TrafficAnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(TrafficAnalyticsController.class);

    @Autowired
    private TrafficAnalyticsService analyticsService;

    @Autowired
    private TrafficService trafficService;

    // Helper method để tạo request object đồng nhất cho các API thống kê lịch sử
    private AnalyticsFilterRequest createFilter(Double lat, Double lng, Double radius, LocalDateTime start, LocalDateTime end) {
        AnalyticsFilterRequest filter = new AnalyticsFilterRequest();
        filter.setLat(lat);
        filter.setLng(lng);
        filter.setRadius(radius);
        filter.setStartDate(start);
        filter.setEndDate(end);
        return filter;
    }

    @GetMapping("/overview")
    public ResponseEntity<TrafficMetricsOverviewResponse> getDashboardOverview(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getOverviewMetrics(createFilter(lat, lng, radius, startDate, endDate)));
    }

    @GetMapping("/incident-trend")
    public ResponseEntity<List<TrafficTimeStatsResponse>> getIncidentTrendData(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getIncidentTrendAnalysis(createFilter(lat, lng, radius, startDate, endDate)));
    }

    @GetMapping("/incident-location")
    public ResponseEntity<List<TrafficLocationStatsResponse>> getIncidentLocationData(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getIncidentLocationDensityAnalysis(createFilter(lat, lng, radius, startDate, endDate)));
    }

    @GetMapping("/speed-evolution")
    public ResponseEntity<List<TrafficSpeedAnalyticsResponse>> getSpeedEvolutionData(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate != null && endDate != null) {
            long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysDiff > 1) {
                startDate = endDate.minusDays(1);
            }
        }

        if (lat == null || lng == null) {
            return ResponseEntity.ok(analyticsService.getHourlySpeedEvolution(createFilter(lat, lng, radius, startDate, endDate)));
        }

        List<TrafficSpeedAnalyticsResponse> dbData = analyticsService.getHourlySpeedEvolution(createFilter(lat, lng, radius, startDate, endDate));
        boolean hasHistoryData = dbData != null && dbData.stream().anyMatch(h -> h.getAverageSpeed() > 0);

        if (hasHistoryData) {
            return ResponseEntity.ok(dbData);
        }

        try {
            TrafficResponse.FlowSegmentData realTimeData = trafficService.getTrafficFlow(lat, lng);

            if (realTimeData != null && realTimeData.getCurrentSpeed() > 0) {
                logger.info("Dữ liệu tốc độ real-time thu được: {} km/h. Đang tạo dữ liệu giờ mô phỏng.", realTimeData.getCurrentSpeed());
                
                List<TrafficSpeedAnalyticsResponse> dynamicSpeedList = new ArrayList<>();
                double currentRealSpeed = (double) realTimeData.getCurrentSpeed();
                int currentHour = LocalDateTime.now().getHour();

                for (int hour = 0; hour < 24; hour++) {
                    double simulatedSpeed;
                    double locationRandomOffset = Math.sin(lat * lng + hour) * 3.5;

                    if (hour == currentHour) {
                        simulatedSpeed = currentRealSpeed;
                    } else if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
                        simulatedSpeed = Math.max(currentRealSpeed * 0.55 + locationRandomOffset, 10.0);
                    } else if (hour >= 23 || hour <= 4) {
                        simulatedSpeed = Math.min(currentRealSpeed * 1.35 + locationRandomOffset, realTimeData.getFreeFlowSpeed());
                    } else {
                        simulatedSpeed = currentRealSpeed * (0.9 + Math.sin(hour) * 0.1) + locationRandomOffset;
                    }

                    double finalSpeed = Math.max(Math.round(simulatedSpeed * 10.0) / 10.0, 5.0);
                    dynamicSpeedList.add(new TrafficSpeedAnalyticsResponse(hour, finalSpeed));
                }
                return ResponseEntity.ok(dynamicSpeedList);
            } else {
                logger.warn("API real-time trả về dữ liệu không hợp lệ. Sử dụng dữ liệu lịch sử trống làm dự phòng.");
            }
        } catch (Exception e) {
            logger.error("Lỗi khi lấy dữ liệu giao thông real-time: {}", e.getMessage(), e);
        }

        logger.warn("Không có dữ liệu tốc độ cho vị trí ({}, {}). Trả về kết quả trống.", lat, lng);
        return ResponseEntity.ok(new ArrayList<>());
    }
}