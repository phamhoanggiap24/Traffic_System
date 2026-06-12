package com.traffic.dto.response;

import lombok.Data;

@Data
public class TrafficSpeedAnalyticsResponse {
    private Integer hour;
    private Double averageSpeed;

    public TrafficSpeedAnalyticsResponse(Integer hour, Double averageSpeed) {
        this.hour = hour;
        this.averageSpeed = averageSpeed;
    }
}