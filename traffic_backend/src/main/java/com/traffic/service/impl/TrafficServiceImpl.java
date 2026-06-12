package com.traffic.service.impl;

import com.traffic.dto.response.TrafficResponse;
import com.traffic.dto.response.RouteResponse;
import com.traffic.entity.DuLieuGiaoThong;
import com.traffic.entity.KhuVuc;
import com.traffic.repository.DuLieuGiaoThongRepository;
import com.traffic.repository.NhatKyHeThongRepository;
import com.traffic.service.TrafficService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Locale;

@Service("trafficService")
@Slf4j
public class TrafficServiceImpl implements TrafficService {

    @Value("${tomtom.api.key}")
    private String apiKey;

    @Value("${tomtom.api.base-url:https://api.tomtom.com/traffic/services/4/flowSegmentData/absolute/10/json}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final DuLieuGiaoThongRepository duLieuGiaoThongRepository;

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository;

    public TrafficServiceImpl(RestTemplate restTemplate, DuLieuGiaoThongRepository duLieuGiaoThongRepository) {
        this.restTemplate = restTemplate;
        this.duLieuGiaoThongRepository = duLieuGiaoThongRepository;
    }

    @Override
    public TrafficResponse.FlowSegmentData getTrafficFlow(double lat, double lon) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("key", apiKey)
                .queryParam("point", lat + "," + lon)
                .toUriString();

        try {
            log.info("Đang gọi TomTom API cho tọa độ: {}, {}", lat, lon);
            TrafficResponse response = restTemplate.getForObject(url, TrafficResponse.class);

            if (response != null && response.getFlowSegmentData() != null) {
                return response.getFlowSegmentData();
            }
        } catch (Exception e) {
            log.error("Lỗi khi kết nối TomTom API: {}", e.getMessage());
        }
        return null;
    }

    @Override
    @Transactional
    public void processAndSaveTrafficData(KhuVuc kv, TrafficResponse.FlowSegmentData realTimeData) {
        if (realTimeData == null) return;

        try {
            DuLieuGiaoThong entity = new DuLieuGiaoThong();
            entity.setKhuVuc(kv);
            int currentSpeed = realTimeData.getCurrentSpeed();
            entity.setTocDoTrungBinh((float) currentSpeed);
            entity.setThoiDiemGhiNhan(LocalDateTime.now());
            entity.setNguonDuLieu("TOMTOM_API");
            entity.setDoTinCayDuLieu(1.0f);

            int freeFlowSpeed = realTimeData.getFreeFlowSpeed();
            if (currentSpeed == 0 || freeFlowSpeed == 0) {
                entity.setMucDoUnTac(5);
            } else {
                float ratio = (float) currentSpeed / freeFlowSpeed;
                if (ratio < 0.3f) entity.setMucDoUnTac(5);
                else if (ratio < 0.5f) entity.setMucDoUnTac(4);
                else if (ratio < 0.7f) entity.setMucDoUnTac(3);
                else if (ratio < 0.9f) entity.setMucDoUnTac(2);
                else entity.setMucDoUnTac(1);
            }

            duLieuGiaoThongRepository.save(entity);
        } catch (Exception e) {
            log.error("Lỗi lưu DB: {}", e.getMessage());
        }
    }

    @Override
    public String calculateRoutes(Double startLat, Double startLng, Double endLat, Double endLng) {
        if (startLat == null || startLng == null || endLat == null || endLng == null) {
            throw new IllegalArgumentException("Tọa độ không được để trống");
        }

        String url = String.format(Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson&alternatives=true",
                startLng, startLat, endLng, endLat
        );

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("Lỗi OSRM: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getStreetName(double lat, double lon) {
        try {
            String url = String.format(Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f&accept-language=vi",
                    lat, lon
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "TrafficMonitoringApp/1.0 (DuanGiamSatGiaoThong)");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);

            String body = response.getBody();
            if (body != null && !body.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(body);

                if (root.has("display_name")) {
                    String fullAddress = root.get("display_name").asText();

                    if (fullAddress.contains(", Việt Nam")) {
                        fullAddress = fullAddress.replace(", Việt Nam", "");
                    }
                    return fullAddress;
                }
            }
        } catch (Exception e) {
            log.error("Lỗi lấy tên đường chi tiết: ", e);
        }
        return "Tọa độ [" + lat + ", " + lon + "]";
    }

    // Tính toán tuyến đường và ETA thời gian thực
    @Override
    public RouteResponse calculateRouteWithRealTimeETA(Double startLat, Double startLng, Double endLat, Double endLng) {
        if (startLat == null || startLng == null || endLat == null || endLng == null) {
            throw new IllegalArgumentException("Tọa độ không được để trống");
        }

        try {
            // Gọi OSRM lấy chuỗi dữ liệu thô
            String jsonResponse = this.calculateRoutes(startLat, startLng, endLat, endLng);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(jsonResponse);
            com.fasterxml.jackson.databind.JsonNode routesNode = root.path("routes");

            if (routesNode == null || !routesNode.isArray() || routesNode.size() == 0) {
                throw new RuntimeException("Không tìm thấy tuyến đường hợp lệ từ OSRM");
            }

            java.util.List<RouteResponse.SingleRoute> calculatedRoutes = new java.util.ArrayList<>();

            // Vòng lặp duyệt qua từng tuyến đường
            for (com.fasterxml.jackson.databind.JsonNode route : routesNode) {
                double totalDistanceMeters = route.path("distance").asDouble();
                com.fasterxml.jackson.databind.JsonNode coordinates = route.path("geometry").path("coordinates");

                // Lấy mẫu 3 điểm dọc theo tuyến đường hiện tại để quét vận tốc
                java.util.List<double[]> samplePoints = new java.util.ArrayList<>();
                int totalCoords = coordinates.size();
                if (totalCoords > 0) {
                    samplePoints.add(new double[]{coordinates.get(0).get(1).asDouble(), coordinates.get(0).get(0).asDouble()});
                    if (totalCoords > 2) {
                        samplePoints.add(new double[]{coordinates.get(totalCoords / 2).get(1).asDouble(), coordinates.get(totalCoords / 2).get(0).asDouble()});
                    }
                    samplePoints.add(new double[]{coordinates.get(totalCoords - 1).get(1).asDouble(), coordinates.get(totalCoords - 1).get(0).asDouble()});
                }

                // Tính toán vận tốc thực tế cho tuyến đường
                double totalSpeed = 0;
                int validPointsCount = 0;
                for (double[] point : samplePoints) {
                    TrafficResponse.FlowSegmentData flow = this.getTrafficFlow(point[0], point[1]);
                    if (flow != null && flow.getCurrentSpeed() > 0) {
                        totalSpeed += flow.getCurrentSpeed();
                        validPointsCount++;
                    }
                }

                double averageSpeedKmH = (validPointsCount > 0) ? (totalSpeed / validPointsCount) : 35.0;
                if (averageSpeedKmH < 5.0) {
                    averageSpeedKmH = 5.0; // Giới hạn 5km/h nếu tắc đường
                }

                // Áp dụng công thức tính ETA cho tuyến đường
                double speedMetersPerSecond = averageSpeedKmH / 3.6;
                double timeInSeconds = totalDistanceMeters / speedMetersPerSecond;
                double etaMinutes = Math.ceil(timeInSeconds / 60.0);

                // Đóng gói tuyến đường này vào danh sách kết quả
                calculatedRoutes.add(RouteResponse.SingleRoute.builder()
                        .geometry(route.path("geometry"))
                        .distanceMeters(totalDistanceMeters)
                        .thoiGianUocTinhPhut(etaMinutes)
                        .build());
            }

            // Trả về đối tượng chứa toàn bộ các tuyến đường đã tính toán xong ETA
            return RouteResponse.builder()
                    .routes(calculatedRoutes)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi tính toán ETA hàng loạt: {}", e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi định tuyến đa tuyến.");
        }
    }
}