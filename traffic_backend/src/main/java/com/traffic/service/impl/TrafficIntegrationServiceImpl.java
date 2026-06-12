package com.traffic.service.impl;

import com.traffic.dto.response.TrafficResponse;
import com.traffic.entity.DuLieuGiaoThong;
import com.traffic.entity.KhuVuc;
import com.traffic.repository.DuLieuGiaoThongRepository;
import com.traffic.repository.KhuVucRepository;
import com.traffic.service.TrafficService;
import com.traffic.service.TrafficIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class TrafficIntegrationServiceImpl implements TrafficIntegrationService {

    @Autowired
    private TrafficService tomTomService;

    @Autowired
    private DuLieuGiaoThongRepository duLieuRepo;

    @Autowired
    private KhuVucRepository khuVucRepo;

    @Override
    @Transactional
    public void updateTrafficData(Double lat, Double lon, Integer khuVucId) {
        if (khuVucId == null || lat == null || lon == null) {
            return;
        }

        TrafficResponse.FlowSegmentData data = tomTomService.getTrafficFlow(lat, lon);

        if (data != null) {
            KhuVuc kv = khuVucRepo.findById(khuVucId.longValue()).orElse(null);

            if (kv == null) {
                return;
            }

            DuLieuGiaoThong entity = new DuLieuGiaoThong();
            entity.setKhuVuc(kv);
            entity.setTocDoTrungBinh((float) data.getCurrentSpeed());

            int mucDo = calculateCongestionLevel(data.getCurrentSpeed(), data.getFreeFlowSpeed());
            entity.setMucDoUnTac(mucDo);

            entity.setNguonDuLieu("TomTom API");
            entity.setThoiDiemGhiNhan(LocalDateTime.now());
            entity.setDoTinCayDuLieu(1.0f);

            duLieuRepo.save(entity);
        }
    }

    private int calculateCongestionLevel(int current, int freeFlow) {
        if (current <= freeFlow * 0.2) return 4; // Ùn tắc nghiêm trọng
        if (current < freeFlow * 0.5) return 3;  // Ùn tắc nặng
        if (current < freeFlow * 0.8) return 2;  // Ùn tắc nhẹ
        return 1;                                // Thông thoáng
    }
}