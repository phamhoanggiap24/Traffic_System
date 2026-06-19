package com.traffic.scheduler;

import com.traffic.common.ReportStatus;
import com.traffic.dto.response.ReportResponse;
import com.traffic.entity.BaoCaoSuCo;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.BaoCaoSuCoRepository;
import com.traffic.repository.TaiKhoanRepository;
import com.traffic.service.EmailService;
import com.traffic.service.TrafficService;
import com.traffic.service.impl.IncidentReportServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ReportStatusScheduler {

    @Autowired
    private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private IncidentReportServiceImpl incidentReportService;

    @Scheduled(fixedRate = 60000)
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
                incidentReportService.notifyNearbyUsers(bc);
                if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    try {
                        String tenDuong = trafficService.getStreetName(bc.getViDo(), bc.getKinhDo());

                        if (tenDuong == null || tenDuong.trim().isEmpty()) {
                            tenDuong = "Vị trí đã ghim trên hệ thống";
                        }

                        ReportResponse emailPayload = ReportResponse.builder()
                                .baoCaoId(bc.getBaoCaoId())
                                .moTa((bc.getMoTa() != null ? bc.getMoTa() : "Không có mô tả")
                                        + " (Báo cáo đã được hệ thống tự động phê duyệt do hết thời gian chờ xác minh.)")
                                .viDo(bc.getViDo())
                                .kinhDo(bc.getKinhDo())
                                .tenDangNhap(user.getTenDangNhap())
                                .tenLoaiSuCo(bc.getLoaiSuCo() != null ? bc.getLoaiSuCo().getTenLoai() : "Sự cố giao thông")
                                .trangThai(bc.getTrangThai())
                                .thoiGianBaoCao(bc.getThoiGianBaoCao())
                                .build();

                        String toEmail = user.getEmail();
                        String finalTenDuong = tenDuong;

                        CompletableFuture.runAsync(() -> {
                            try {
                                emailService.sendTrafficIncidentAlert(toEmail, emailPayload, finalTenDuong);
                            } catch (Exception e) {
                                System.err.println("Lỗi gửi email scheduler tự động duyệt nền: " + e.getMessage());
                            }
                        });

                    } catch (Exception e) {
                        log.error("Lỗi đóng gói email scheduler cho báo cáo #{}: {}", bc.getBaoCaoId(), e.getMessage());
                    }
                }
                log.info("Báo cáo Chờ xác minh #{} đã hết thời gian chờ -> Hệ thống tự động duyệt.", bc.getBaoCaoId());
            }

            log.info("[HỆ THỐNG] Hoàn thành tiến trình quét định kỳ.");
        } catch (Exception e) {
            log.error("Lỗi Scheduler: {}", e.getMessage());
        }
    }
}