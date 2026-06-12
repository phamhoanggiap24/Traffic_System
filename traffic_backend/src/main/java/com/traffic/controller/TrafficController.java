package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.RoutingRequest;
import com.traffic.dto.response.RouteResponse;
import com.traffic.dto.response.RoutingResponse;
import com.traffic.dto.response.TrafficResponse;
import com.traffic.service.TrafficService;
import com.traffic.service.TrafficIntegrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "*")
public class TrafficController {

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private TrafficIntegrationService trafficIntegrationService;

    // Chỉ lấy dữ liệu xem nhanh
    @GetMapping("/flow-raw")
    public ResponseEntity<ApiResponse<TrafficResponse.FlowSegmentData>> getRawFlow(
            @RequestParam double lat, @RequestParam double lon) {

        TrafficResponse.FlowSegmentData data = trafficService.getTrafficFlow(lat, lon);

        if (data != null) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Lấy dữ liệu TomTom thành công", data));
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, "Không tìm thấy dữ liệu tại vị trí này", null));
    }

    // Lấy dữ liệu và lưu vào Database
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Void>> updateAndGetFlow(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam Integer khuVucId) {

        trafficIntegrationService.updateTrafficData(lat, lon, khuVucId);

        return ResponseEntity.ok(new ApiResponse<>(200, "Hệ thống đã ghi nhận dữ liệu giao thông mới", null));
    }

    // Tính toán tuyến đường
    @PostMapping("/calculate-route")
    public ResponseEntity<RoutingResponse> getRoutes(@RequestBody RoutingRequest request) {
        try {
            String routeJson = trafficService.calculateRoutes(
                    request.getStartLat(), request.getStartLng(),
                    request.getEndLat(), request.getEndLng()
            );

            ObjectMapper mapper = new ObjectMapper();
            Object jsonObject = mapper.readTree(routeJson);

            return ResponseEntity.ok(new RoutingResponse(true, "Tính toán tuyến đường thành công", jsonObject));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new RoutingResponse(false, "Lỗi hệ thống: " + e.getMessage(), null));
        }
    }

    // Tìm đường kèm ETA thời gian thực
    @GetMapping("/route-eta")
    public ResponseEntity<ApiResponse<RouteResponse>> getRouteWithETA(
            @RequestParam double sLat,
            @RequestParam double sLng,
            @RequestParam double eLat,
            @RequestParam double eLng) {
        try {
            RouteResponse response = trafficService.calculateRouteWithRealTimeETA(sLat, sLng, eLat, eLng);

            if (response != null) {
                return ResponseEntity.ok(new ApiResponse<>(200, "Tính toán lộ trình và ETA thời gian thực thành công", response));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Không thể tính toán lộ trình cho các tọa độ này", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Lỗi hệ thống khi định tuyến: " + e.getMessage(), null));
        }
    }
}