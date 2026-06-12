package com.traffic.dto.response;

import lombok.Data;
import java.sql.Date;

@Data
public class TrafficTimeStatsResponse {
    private Date reportDate;
    private Long totalReports;

    public TrafficTimeStatsResponse(Object reportDate, Object totalReports) {
        this.reportDate = (Date) reportDate;
        this.totalReports = ((Number) totalReports).longValue();
    }
}