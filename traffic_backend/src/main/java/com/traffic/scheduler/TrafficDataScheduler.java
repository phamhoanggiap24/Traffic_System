package com.traffic.scheduler;

import com.traffic.dto.response.TrafficResponse;
import com.traffic.entity.KhuVuc;
import com.traffic.repository.KhuVucRepository;
import com.traffic.service.TrafficService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@Slf4j
public class TrafficDataScheduler {

    @Autowired
    private KhuVucRepository khuVucRepository;

    @Autowired
    private TrafficService trafficService;

    @Scheduled(fixedRate = 900000)
    public void autoFetchAndSaveTrafficData() {
        log.info("[CRON JOB] Bắt đầu chu kỳ quét tọa độ đồng bộ dữ liệu.");

        List<KhuVuc> danhSachKhuVuc = khuVucRepository.findAll();
        if (danhSachKhuVuc.isEmpty()) {
            log.warn("Chưa có khu vực nào trong bảng 'khu_vuc'!");
            return;
        }

        for (KhuVuc kv : danhSachKhuVuc) {
            if (kv.getViDo() == null || kv.getKinhDo() == null) continue;

            try {
                // 1. Gọi API lấy dữ liệu thực tế
                TrafficResponse.FlowSegmentData realTimeData = trafficService.getTrafficFlow(kv.getViDo(), kv.getKinhDo());

                // 2. Đẩy sang Service lo liệu việc tính toán kẹt xe và lưu trữ vào MySQL
                trafficService.processAndSaveTrafficData(kv, realTimeData);

                log.info("Đã đồng bộ dữ liệu tự động cho khu vực: {}", kv.getTenKhuVuc());
            } catch (Exception e) {
                log.error("Lỗi khi chạy tự động cho khu vực {}: {}", kv.getTenKhuVuc(), e.getMessage());
            }
        }
        log.info(">>> [CRON JOB] Hoàn thành chu kỳ đồng bộ dữ liệu.");
    }
}