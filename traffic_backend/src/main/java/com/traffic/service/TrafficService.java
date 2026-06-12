package com.traffic.service;

import com.traffic.dto.response.RouteResponse;
import com.traffic.dto.response.TrafficResponse;
import com.traffic.entity.KhuVuc;

public interface TrafficService {
    TrafficResponse.FlowSegmentData getTrafficFlow(double lat, double lon);
    void processAndSaveTrafficData(KhuVuc kv, TrafficResponse.FlowSegmentData realTimeData);
    String calculateRoutes(Double startLat, Double startLng, Double endLat, Double endLng);
    String getStreetName(double lat, double lon);
    RouteResponse calculateRouteWithRealTimeETA(Double startLat, Double startLng, Double endLat, Double endLng);
}