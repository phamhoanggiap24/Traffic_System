package com.traffic.service;

import com.traffic.dto.request.AnalyticsFilterRequest;
import com.traffic.dto.response.*;

import java.util.List;

public interface TrafficAnalyticsService {

    // Lấy dữ liệu tổng quan cho các thẻ số liệu nhanh
    TrafficMetricsOverviewResponse getOverviewMetrics(AnalyticsFilterRequest filter);

    // Lấy dữ liệu xu hướng số lượng sự cố theo chuỗi thời gian ngày
    List<TrafficTimeStatsResponse> getIncidentTrendAnalysis(AnalyticsFilterRequest filterRequest);

    // Lấy danh sách phân tích mật độ sự cố giao thông theo phân vùng khu vực
    List<TrafficLocationStatsResponse> getIncidentLocationDensityAnalysis(AnalyticsFilterRequest filter);

    // Lấy diễn biến vận tốc dòng xe chạy theo khung giờ trong ngày
    List<TrafficSpeedAnalyticsResponse> getHourlySpeedEvolution(AnalyticsFilterRequest filterRequest);
}