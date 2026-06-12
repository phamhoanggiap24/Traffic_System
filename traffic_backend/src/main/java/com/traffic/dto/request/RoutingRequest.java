package com.traffic.dto.request;

import lombok.Data;

@Data
public class RoutingRequest {
    private Double startLat;
    private Double startLng;
    private Double endLat;
    private Double endLng;
}