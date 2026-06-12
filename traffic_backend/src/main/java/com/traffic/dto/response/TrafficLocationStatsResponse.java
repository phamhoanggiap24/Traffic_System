package com.traffic.dto.response;

import lombok.Data;

@Data
public class TrafficLocationStatsResponse {
    private String locationName;
    private Long totalReports;
    private Long verifiedReports;

    public TrafficLocationStatsResponse(String locationName, Long totalReports, Long verifiedReports) {
        this.locationName = locationName;
        this.totalReports = totalReports;
        this.verifiedReports = verifiedReports;
    }

}