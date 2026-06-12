package com.traffic.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TrafficResponse {
    @JsonProperty("flowSegmentData")
    private FlowSegmentData flowSegmentData;

    @Data
    public static class FlowSegmentData {
        private int currentSpeed;       // Tốc độ hiện tại (km/h)
        private int freeFlowSpeed;      // Tốc độ khi đường vắng (km/h)
        private int currentTravelTime;  // Thời gian đi qua đoạn đường (giây)
        private int freeFlowTravelTime; // Thời gian lý tưởng (giây)
        private String confidence;      // Độ tin cậy của dữ liệu (0.0 - 1.0)
    }
}