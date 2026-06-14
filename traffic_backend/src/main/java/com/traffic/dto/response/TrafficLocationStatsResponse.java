package com.traffic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficLocationStatsResponse {
    private String locationName;
    private Long totalReports;
    private Long verifiedReports;

    public TrafficLocationStatsResponse(Object locationName, Object totalReports, Object verifiedReports) {
        this.locationName = locationName != null ? locationName.toString() : "Không xác định";
        this.totalReports = totalReports != null ? ((Number) totalReports).longValue() : 0L;
        this.verifiedReports = verifiedReports != null ? ((Number) verifiedReports).longValue() : 0L;
    }
}