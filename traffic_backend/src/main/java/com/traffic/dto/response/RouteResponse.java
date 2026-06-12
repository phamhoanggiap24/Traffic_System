package com.traffic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    private List<SingleRoute> routes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleRoute {
        private Object geometry; // Chứa mảng tọa độ vẽ đường đi
        private Double distanceMeters; // Quãng đường
        private Double thoiGianUocTinhPhut; // Số phút ETA
    }
}