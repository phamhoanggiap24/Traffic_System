package com.traffic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficTimeStatsResponse {
    private String reportDate;
    private Long totalReports;

    public TrafficTimeStatsResponse(Object reportDate, Object totalReports) {
        this.reportDate = reportDate != null ? reportDate.toString() : "";
        this.totalReports = totalReports != null ? ((Number) totalReports).longValue() : 0L;
    }
}