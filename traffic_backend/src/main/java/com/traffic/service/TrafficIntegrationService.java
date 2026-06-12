package com.traffic.service;

public interface TrafficIntegrationService {
    // Cập nhật dữ liệu giao thông từ nguồn bên ngoài và lưu vào DB
    void updateTrafficData(Double lat, Double lon, Integer khuVucId);
}