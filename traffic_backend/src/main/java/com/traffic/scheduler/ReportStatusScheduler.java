package com.traffic.scheduler;

import com.traffic.common.ReportStatus;
import com.traffic.entity.BaoCaoSuCo;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.BaoCaoSuCoRepository;
import com.traffic.repository.TaiKhoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class ReportStatusScheduler {

    @Autowired
    private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void processAutoVerifyAndExpired() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[HỆ THỐNG] Đang tiến hành quét các báo cáo Chờ xác minh.");

        try {
            // XỬ LÝ NHÁNH CHỜ XÁC MINH
            List<BaoCaoSuCo> pendingOverdue = baoCaoSuCoRepository.findPendingReportsOverdue(now);
            for (BaoCaoSuCo bc : pendingOverdue) {
                bc.setTrangThai(ReportStatus.DA_XAC_MINH);
                bc.setThoiGianXacMinh(now);
                bc.setDoTinCayBaoCao(50);

                TaiKhoan user = bc.getTaiKhoan();
                if (user != null) {
                    int currentPoint = (user.getDoTinCayNguoiDung() != null) ? user.getDoTinCayNguoiDung() : 0;
                    if (currentPoint < 50) {
                        user.setDoTinCayNguoiDung(currentPoint + 1);
                        taiKhoanRepository.save(user);
                    }
                }
                baoCaoSuCoRepository.save(bc);
                log.info("Báo cáo Chờ xác minh #{} đã hết thời gian chờ -> Hệ thống tự động duyệt.", bc.getBaoCaoId());
            }

            log.info("[HỆ THỐNG] Hoàn thành tiến trình quét định kỳ.");
        } catch (Exception e) {
            log.error("Lỗi Scheduler: {}", e.getMessage());
        }
    }
}