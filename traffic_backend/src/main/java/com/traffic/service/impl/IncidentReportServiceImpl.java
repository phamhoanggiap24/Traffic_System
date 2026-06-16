package com.traffic.service.impl;

import com.traffic.common.ReadStatus;
import com.traffic.common.ReportStatus;
import java.util.concurrent.CompletableFuture;
import com.traffic.dto.request.ReportRequest;
import com.traffic.dto.response.ReportResponse;
import com.traffic.entity.*;
import com.traffic.repository.*;
import com.traffic.service.EmailService;
import com.traffic.service.IncidentReportService;
import com.traffic.service.TrafficService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class IncidentReportServiceImpl implements IncidentReportService {

    @Autowired
    private BaoCaoSuCoRepository baoCaoSuCoRepository;

    @Autowired
    private TrafficService trafficService;

    @Autowired
    private LoaiSuCoRepository loaiSuCoRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private CanhBaoRepository canhBaoRepository;

    @Autowired
    private NhatKyXacMinhRepository nhatKyXacMinhRepository;

    @Autowired
    private EmailService emailService;

    // XỬ LÝ LƯU FILE ẢNH MULTIPART VÀO THƯ MỤC CỤC BỘ
    private String saveImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "";
        }
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            } else {
                extension = ".jpg";
            }

            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(fileName);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + fileName;
        } catch (IOException e) {
            System.err.println("Lỗi khi thực hiện lưu file hình ảnh Multipart: " + e.getMessage());
            return "";
        }
    }

    private ReportResponse mapToDTO(BaoCaoSuCo entity) {
        return ReportResponse.builder()
                .baoCaoId(entity.getBaoCaoId())
                .moTa(entity.getMoTa())
                .viDo(entity.getViDo())
                .kinhDo(entity.getKinhDo())
                .hinhAnhUrl(entity.getHinhAnhUrl())
                .trangThai(entity.getTrangThai() != null ? entity.getTrangThai() : ReportStatus.CHO_XAC_MINH)
                .thoiGianBaoCao(entity.getThoiGianBaoCao())
                .tenLoaiSuCo(entity.getLoaiSuCo() != null ? entity.getLoaiSuCo().getTenLoai() : "N/A")
                .tenDangNhap(entity.getTaiKhoan() != null ? entity.getTaiKhoan().getTenDangNhap() : "N/A")
                .build();
    }

    // XÓA BÁO CÁO TRONG DANH SÁCH QUẢN TRỊ
    @Override
    @Transactional
    public void deleteReport(Long id) {
        BaoCaoSuCo bc = baoCaoSuCoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo để xóa"));

        try {
            TaiKhoan user = bc.getTaiKhoan();

            if (user != null && bc.getLoaiSuCo() != null) {
                LocalDateTime now = LocalDateTime.now();
                long loaiId = bc.getLoaiSuCo().getLoaiSuCoId();
                boolean conHan = false;

                if ((loaiId == 1 || loaiId == 2) && bc.getThoiGianBaoCao().plusHours(3).isAfter(now)) {
                    conHan = true;
                }
                else if ((loaiId == 3 || loaiId == 4) && bc.getThoiGianBaoCao().plusDays(1).isAfter(now)) {
                    conHan = true;
                }

                if (conHan) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                    String gioDep = bc.getThoiGianBaoCao().format(formatter);

                    CanhBao thongBaoGo = new CanhBao();
                    thongBaoGo.setNoiDung("Báo cáo về [" + bc.getLoaiSuCo().getTenLoai()
                            + "] gửi lúc " + gioDep
                            + " của bạn đã bị Quản trị viên gỡ bỏ hoàn toàn khỏi hệ thống.");
                    thongBaoGo.setThoiGianGui(now);
                    thongBaoGo.setTrangThai(ReadStatus.CHUA_DOC);
                    thongBaoGo.setLoaiCanhBao("HE_THONG");
                    thongBaoGo.setKenhGui("APP");
                    thongBaoGo.setTaiKhoan(user);
                    thongBaoGo.setBaoCaoSuCo(bc);

                    canhBaoRepository.save(thongBaoGo);
                }
            }

            baoCaoSuCoRepository.deleteReportSoft(id);

        } catch (Exception e) {
            System.err.println("Lỗi khi thực hiện xóa báo cáo: " + e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi gỡ báo cáo: " + e.getMessage());
        }
    }

    // GỠ BÁO CÁO KHỎI BẢN ĐỒ CÔNG KHAI
    @Override
    @Transactional
    public void removeReportFromMap(Long id) {
        BaoCaoSuCo bc = baoCaoSuCoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo để gỡ khỏi bản đồ"));

        try {
            bc.setTrangThai(ReportStatus.AN_HIEN_THI);
            baoCaoSuCoRepository.save(bc);

            TaiKhoan user = bc.getTaiKhoan();
            if (user != null && bc.getLoaiSuCo() != null) {
                LocalDateTime now = LocalDateTime.now();
                long loaiId = bc.getLoaiSuCo().getLoaiSuCoId();
                boolean conHan = false;

                if ((loaiId == 1 || loaiId == 2) && bc.getThoiGianBaoCao().plusHours(3).isAfter(now)) {
                    conHan = true;
                }
                else if ((loaiId == 3 || loaiId == 4) && bc.getThoiGianBaoCao().plusDays(1).isAfter(now)) {
                    conHan = true;
                }

                if (conHan) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                    String gioDep = bc.getThoiGianBaoCao().format(formatter);

                    CanhBao thongBaoGo = new CanhBao();
                    thongBaoGo.setNoiDung("Sự cố [" + bc.getLoaiSuCo().getTenLoai()
                            + "] bạn báo cáo lúc " + gioDep
                            + " đã được xử lý ổn định hoặc giải tỏa cản trở. Cảm ơn sự đóng góp của bạn!");
                    thongBaoGo.setThoiGianGui(now);
                    thongBaoGo.setTrangThai(ReadStatus.CHUA_DOC);
                    thongBaoGo.setLoaiCanhBao("HE_THONG");
                    thongBaoGo.setKenhGui("APP");
                    thongBaoGo.setTaiKhoan(user);
                    thongBaoGo.setBaoCaoSuCo(bc);

                    canhBaoRepository.save(thongBaoGo);
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi gỡ hiển thị báo cáo trên bản đồ: " + e.getMessage());
            throw new RuntimeException("Lỗi hệ thống khi gỡ bản đồ: " + e.getMessage());
        }
    }

    // XỬ LÝ KHI NGƯỜI DÙNG GỬI BÁO CÁO MỚI
    @Override
    @Transactional
    public ReportResponse processReportSubmission(ReportRequest request) {
        BaoCaoSuCo entity = new BaoCaoSuCo();
        entity.setMoTa(request.getMoTa());
        entity.setViDo(request.getViDo());
        entity.setKinhDo(request.getKinhDo());

        String processedUrl = saveImageFile(request.getHinhAnh());
        entity.setHinhAnhUrl(processedUrl);

        entity.setThoiGianBaoCao(LocalDateTime.now());
        entity.setDoTinCayBaoCao(50);
        entity.setTrangThai(ReportStatus.CHO_XAC_MINH); // Mặc định ban đầu là chờ duyệt

        if (request.getTaiKhoanId() != null) {
            TaiKhoan user = taiKhoanRepository.findById(request.getTaiKhoanId()).orElse(null);
            entity.setTaiKhoan(user);
        }

        LoaiSuCo loaiThat = loaiSuCoRepository.findById(request.getLoaiSuCoId())
                .orElseThrow(() -> new RuntimeException("Loại sự cố không tồn tại"));
        entity.setLoaiSuCo(loaiThat);

        boolean isAutoMerged = false;

        try {
            // Lấy dữ liệu dòng chảy giao thông thời gian thực từ TomTom API
            var realTimeData = trafficService.getTrafficFlow(entity.getViDo(), entity.getKinhDo());
            if (realTimeData != null && realTimeData.getFreeFlowSpeed() > 0) {
                double speedRatio = (double) realTimeData.getCurrentSpeed() / realTimeData.getFreeFlowSpeed();

                // 1. KỊCH BẢN DUYỆT TỰ ĐỘNG: Tốc độ xe giảm sâu dưới 40% (Áp dụng cho tất cả loại sự cố)
                if (speedRatio < 0.4) {
                    entity.setTrangThai(ReportStatus.DA_XAC_MINH);
                    entity.setDoTinCayBaoCao(50);
                    entity.setThoiGianXacMinh(LocalDateTime.now());

                    // Kích hoạt tự động gộp nếu trùng vị trí bán kính 50m ngoài bản đồ công khai
                    isAutoMerged = handleMergeIncident(entity);

                    // Tăng 1 điểm uy tín cho người dùng vì đã báo cáo chính xác thực địa
                    if (entity.getTrangThai() == ReportStatus.DA_XAC_MINH && entity.getTaiKhoan() != null && entity.getTaiKhoan().getDoTinCayNguoiDung() < 50) {
                        entity.getTaiKhoan().setDoTinCayNguoiDung(entity.getTaiKhoan().getDoTinCayNguoiDung() + 1);
                    }
                }
                // 2. KỊCH BẢN HẠ UY TÍN/NGHI VẤN: Đường thông thoáng > 80% tốc độ tự do (Áp dụng cho tất cả loại sự cố)
                else if (speedRatio > 0.8) {
                    // Hệ thống nghi ngờ tin giả vì dòng xe vẫn lưu thông rất mượt mà
                    entity.setTrangThai(ReportStatus.NGHI_VAN);
                    entity.setDoTinCayBaoCao(10); // Hạ thấp độ tin cậy của báo cáo

                    System.out.println("[TomTom-Check] Phát hiện nghi vấn tin giả! Loại sự cố: ["
                            + loaiThat.getTenLoai() + "], Vận tốc dòng xe bình thường (Tỷ lệ: " + speedRatio + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi xác minh tự động TomTom: " + e.getMessage());
        }

        BaoCaoSuCo saved = baoCaoSuCoRepository.save(entity);
        TaiKhoan user = saved.getTaiKhoan();

        // LUỒNG TỰ ĐỘNG GỬI EMAIL PHẢN HỒI KHI ĐƯỢC HỆ THỐNG XÁC THỰC THÀNH CÔNG (CHỈ GỬI KHI ĐÃ XÁC MINH)
        if (saved.getTrangThai() == ReportStatus.DA_XAC_MINH && user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            try {
                double lat = saved.getViDo();
                double lng = saved.getKinhDo();
                String tenDuongXacThuc = trafficService.getStreetName(lat, lng);

                if (tenDuongXacThuc == null || tenDuongXacThuc.trim().isEmpty()) {
                    tenDuongXacThuc = "Vị trí đã ghim trên hệ thống";
                }

                ReportResponse emailPayload = ReportResponse.builder()
                        .baoCaoId(saved.getBaoCaoId())
                        .moTa(saved.getMoTa())
                        .viDo(saved.getViDo())
                        .kinhDo(saved.getKinhDo())
                        .tenDangNhap(user.getTenDangNhap())
                        .tenLoaiSuCo(saved.getLoaiSuCo() != null ? saved.getLoaiSuCo().getTenLoai() : "Sự cố giao thông")
                        .trangThai(saved.getTrangThai())
                        .thoiGianBaoCao(saved.getThoiGianBaoCao())
                        .build();

                if (isAutoMerged) {
                    emailPayload.setMoTa(saved.getMoTa() + " (Hệ thống đã tự động xác minh dựa trên phân tích lưu lượng giao thông thực tế và tiến hành gộp báo cáo này vào sự cố lân cận trong bán kính 50m).");
                } else {
                    emailPayload.setMoTa(saved.getMoTa() + " (Báo cáo đã được hệ thống tự động kiểm tra và phê duyệt thành công dựa trên dữ liệu lưu thông thực tế tại khu vực).");
                }

                String toEmail = user.getEmail();
                String finalTenDuong = tenDuongXacThuc;

                System.out.println("[Auto-Email] Kích hoạt bắn mail tự động duyệt cho sự cố [" + emailPayload.getTenLoaiSuCo() + "] tới: " + toEmail);

                CompletableFuture.runAsync(() -> {
                    try {
                        emailService.sendTrafficIncidentAlert(toEmail, emailPayload, finalTenDuong);
                    } catch (Exception e) {
                        System.err.println("Lỗi gửi email tự động duyệt báo cáo nền: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                System.err.println("Lỗi luồng đóng gói dữ liệu gửi mail tự động tổng hợp: " + e.getMessage());
            }
        }

        return mapToDTO(saved);
    }

    // XÁC MINH VÀ DUYỆT/TỪ CHỐI BÁO CÁO SỰ CỐ
    @Override
    @Transactional
    public void verifyReport(Long id, String newStatus) {
        BaoCaoSuCo bc = baoCaoSuCoRepository.findById(id).orElseThrow();

        ReportStatus targetStatus = ReportStatus.valueOf(newStatus);

        if (bc.getTrangThai() == targetStatus) {
            return;
        }

        ReportStatus trangThaiCu = bc.getTrangThai();
        bc.setTrangThai(targetStatus);
        bc.setThoiGianXacMinh(LocalDateTime.now());

        // Nếu Admin chuyển trạng thái thành DA_XAC_MINH
        boolean isMerged = false;
        if (targetStatus == ReportStatus.DA_XAC_MINH) {
            isMerged = handleMergeIncident(bc);
        }

        TaiKhoan user = bc.getTaiKhoan();
        if (user != null) {
            if (bc.getTrangThai() == ReportStatus.DA_XAC_MINH) {
                bc.setDoTinCayBaoCao(50);
                if (trangThaiCu != ReportStatus.DA_XAC_MINH && user.getDoTinCayNguoiDung() < 50) {
                    user.setDoTinCayNguoiDung(user.getDoTinCayNguoiDung() + 1);
                }
            } else if (targetStatus == ReportStatus.SAI_SU_THAT) {
                bc.setDoTinCayBaoCao(0);
                if (trangThaiCu != ReportStatus.SAI_SU_THAT) {
                    int diemHienTai = user.getDoTinCayNguoiDung();
                    int diemMoi = Math.max(0, diemHienTai - 5);
                    user.setDoTinCayNguoiDung(diemMoi);
                }
            }
            taiKhoanRepository.save(user);
        }
        baoCaoSuCoRepository.save(bc);

        // TỰ ĐỘNG LƯU VẾT VÀO NHẬT KÝ XÁC MINH
        try {
            NhatKyXacMinh nhatKy = new NhatKyXacMinh();
            nhatKy.setBaoCao(bc);
            nhatKy.setTaiKhoan(user);
            nhatKy.setHanhDong(bc.getTrangThai().name());

            String lyDoLog = "Thay đổi trạng thái từ [" + trangThaiCu + "] sang [" + targetStatus + "]"
                    + (isMerged ? " (Đã gộp vào sự cố lân cận 50m)" : "");
            nhatKy.setLyDo(lyDoLog);

            nhatKyXacMinhRepository.save(nhatKy);
        } catch (Exception e) {
            System.err.println("Lỗi ghi nhật ký xác minh: " + e.getMessage());
        }

        if (user != null) {
            try {
                CanhBao thongBaoUser = new CanhBao();
                String noiDungThongBao = "";

                if (targetStatus == ReportStatus.DA_XAC_MINH) {
                    noiDungThongBao = "Báo cáo về [" + (bc.getLoaiSuCo() != null ? bc.getLoaiSuCo().getTenLoai() : "Sự cố")
                            + "] của bạn đã được phê duyệt" + (isMerged ? " và gộp vào sự cố đang diễn ra." : " thành công.") + " Cảm ơn đóng góp của bạn!";
                } else if (targetStatus == ReportStatus.SAI_SU_THAT) {
                    noiDungThongBao = "Báo cáo về [" + (bc.getLoaiSuCo() != null ? bc.getLoaiSuCo().getTenLoai() : "Sự cố")
                            + "] của bạn bị từ chối do xác định sai sự thật. Điểm tin cậy của bạn đã bị trừ.";
                } else {
                    noiDungThongBao = "Báo cáo sự cố mã #" + bc.getBaoCaoId() + " của bạn đã được cập nhật trạng thái sang: " + newStatus;
                }

                thongBaoUser.setNoiDung(noiDungThongBao);
                thongBaoUser.setThoiGianGui(LocalDateTime.now());
                thongBaoUser.setTrangThai(ReadStatus.CHUA_DOC);
                thongBaoUser.setLoaiCanhBao("HE_THONG");
                thongBaoUser.setKenhGui("APP");
                thongBaoUser.setTaiKhoan(user);
                thongBaoUser.setBaoCaoSuCo(bc);

                canhBaoRepository.save(thongBaoUser);
            } catch (Exception e) {
                System.err.println("Lỗi tự động tạo thông báo phản hồi cho User: " + e.getMessage());
            }

            // TỰ ĐỘNG GỬI EMAIL
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                try {
                    // Gọi lấy tên đường từ tọa độ thực tế thông qua TomTom API
                    double lat = bc.getViDo();
                    double lng = bc.getKinhDo();
                    String tenDuongXacThuc = trafficService.getStreetName(lat, lng);

                    if (tenDuongXacThuc == null || tenDuongXacThuc.trim().isEmpty()) {
                        tenDuongXacThuc = "Vị trí đã ghim trên hệ thống";
                    }

                    // Chuẩn bị payload DTO chuyển sang Mail Service
                    ReportResponse emailPayload = ReportResponse.builder()
                            .baoCaoId(bc.getBaoCaoId())
                            .moTa(bc.getMoTa())
                            .viDo(bc.getViDo())
                            .kinhDo(bc.getKinhDo())
                            .tenDangNhap(user.getTenDangNhap())
                            .tenLoaiSuCo(bc.getLoaiSuCo() != null ? bc.getLoaiSuCo().getTenLoai() : "Sự cố giao thông")
                            .trangThai(bc.getTrangThai())
                            .thoiGianBaoCao(bc.getThoiGianBaoCao())
                            .build();

                    // Tùy biến tiêu đề và nội dung động dựa theo trạng thái xử lý của Admin
                    if (targetStatus == ReportStatus.DA_XAC_MINH) {
                        if (isMerged) {
                            emailPayload.setMoTa(bc.getMoTa() + " (Hệ thống đã tự động gộp báo cáo này vào sự cố tương tự đang diễn ra trong phạm vi 50m).");
                        }
                        String toEmail = user.getEmail();
                        String finalTenDuong = tenDuongXacThuc;

                        System.out.println("[Email-Trigger] Kích hoạt gửi thư duyệt báo cáo thành công tới: " + toEmail);

                        CompletableFuture.runAsync(() -> {
                            try {
                                emailService.sendTrafficIncidentAlert(toEmail, emailPayload, finalTenDuong);
                            } catch (Exception e) {
                                System.err.println("Lỗi gửi email duyệt báo cáo nền: " + e.getMessage());
                            }
                        });
                    }
                    else if (targetStatus == ReportStatus.SAI_SU_THAT) {
                        // Tạo một tiêu đề/nội dung cảnh cáo tin giả gửi sang mail
                        System.out.println("[Email-Trigger] Kích hoạt gửi thư TỪ CHỐI (Tin giả) tới: " + user.getEmail());

                        // Để tận dụng hàm mẫu HTML có sẵn, ta cập nhật tạm thuộc tính hiển thị loại sự cố thành tiêu đề cảnh báo
                        emailPayload.setTenLoaiSuCo(bc.getLoaiSuCo().getTenLoai() + " (BỊ TỪ CHỐI)");
                        if (emailPayload.getMoTa() == null || emailPayload.getMoTa().isEmpty()) {
                            emailPayload.setMoTa("Không có mô tả.");
                        }
                        emailPayload.setMoTa(emailPayload.getMoTa() + " - [Lý do từ chối: Xác định thông tin sai sự thật].");

                        String toEmail = user.getEmail();
                        String finalTenDuong = tenDuongXacThuc;

                        CompletableFuture.runAsync(() -> {
                            try {
                                emailService.sendTrafficIncidentAlert(toEmail, emailPayload, finalTenDuong);
                            } catch (Exception e) {
                                System.err.println("Lỗi gửi email từ chối báo cáo nền: " + e.getMessage());
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi nghiêm trọng trong luồng đóng gói dữ liệu gửi mail: " + e.getMessage());
                }
            }
        }
    }

    // LẤY DANH SÁCH BÁO CÁO PHÂN TRANG KÈM BỘ LỌC TÌM KIẾM
    @Override
    public Page<ReportResponse> getFilteredReportsPage(Integer incidentTypeId, String username, String status, String date, Pageable pageable) {
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (date != null && !date.trim().isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(date.trim());
                start = localDate.atStartOfDay();
                end = localDate.atTime(23, 59, 59);
            } catch (Exception e) { System.err.println("Lỗi parse ngày: " + e.getMessage()); }
        }

        // Tính toán mốc thời gian cố định bằng Java
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeLimit30 = now.minusMinutes(30);
        LocalDateTime timeLimit60 = now.minusMinutes(60);

        // LỌC TÌM KIẾM THEO TAB "QUÁ HẠN" ảo
        if ("QUA_HAN".equals(status)) {
            return baoCaoSuCoRepository.findExpiredReports(
                            incidentTypeId, username, start, end, timeLimit30, timeLimit60, pageable)
                    .map(item -> {
                        ReportResponse dto = mapToDTO(item);
                        dto.setTrangThai(ReportStatus.valueOf("NGHI_VAN"));
                        return dto;
                    });
        }

        // LỌC TÌM KIẾM THEO TAB "NGHI VẤN" (Chỉ lấy các bài chưa quá hạn, KO dùng chung findWithFilters nữa)
        if ("NGHI_VAN".equals(status)) {
            return baoCaoSuCoRepository.findActiveSuspectReports(
                            incidentTypeId, username, start, end, timeLimit30, timeLimit60, pageable)
                    .map(this::mapToDTO); // Trả ra data sạch 100%, không bao giờ chứa phần tử null
        }

        // CÁC TAB CÒN LẠI (CHO_XAC_MINH, DA_XAC_MINH, SAI_SU_THAT...)
        ReportStatus enumStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try { enumStatus = ReportStatus.valueOf(status.trim()); } catch (Exception e) {}
        }

        Page<BaoCaoSuCo> reportsPage = baoCaoSuCoRepository.findWithFilters(incidentTypeId, username, enumStatus, start, end, pageable);
        return reportsPage.map(this::mapToDTO);
    }

    // LẤY DANH SÁCH CÁC BÁO CÁO ĐANG HOẠT ĐỘNG ĐỂ HIỂN THỊ LÊN BẢN ĐỒ CÔNG KHAI
    @Override
    public List<ReportResponse> getPublicMapReports() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeHoursAgo = now.minusHours(3);
        LocalDateTime oneDayAgo = now.minusDays(1);
        return baoCaoSuCoRepository.findActiveReportsForMap(threeHoursAgo, oneDayAgo)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // Hàm lấy danh sách báo cáo Quá hạn thực tế
    @Override
    public Page<ReportResponse> getExpiredReportsPage(Integer loaiSuCoId, String tenDangNhap, String ngay, Pageable pageable) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (ngay != null && !ngay.trim().isEmpty()) {
            try {
                LocalDate localDate = LocalDate.parse(ngay.trim());
                start = localDate.atStartOfDay();
                end = localDate.atTime(LocalTime.MAX);
            } catch (Exception e) {
                System.err.println("Lỗi parse ngày mục quá hạn: " + e.getMessage());
            }
        }

        // TÍNH TOÁN MỐC THỜI GIAN TRƯỚC KHI TRUYỀN VÀO REPOSITORY JPQL
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeLimit30 = now.minusMinutes(30);
        LocalDateTime timeLimit60 = now.minusMinutes(60);

        Page<BaoCaoSuCo> expiredPage = baoCaoSuCoRepository.findExpiredReports(
                loaiSuCoId, tenDangNhap, start, end, timeLimit30, timeLimit60, pageable
        );

        return expiredPage.map(this::mapToDTO);
    }

    // ĐẾM SỐ LƯỢNG BÁO CÁO CHƯA DUYỆT
    @Override
    public long getPendingReportsCount(LocalDateTime now) {
        LocalDateTime serverTime = (now != null) ? now : LocalDateTime.now();
        return baoCaoSuCoRepository.countPendingReports(serverTime);
    }

    private boolean handleMergeIncident(BaoCaoSuCo newIncident) {
        if (newIncident == null || newIncident.getLoaiSuCo() == null) {
            return false;
        }

        // Tìm kiếm các báo cáo cùng loại, đang hiển thị trên bản đồ trong bán kính 50m
        List<BaoCaoSuCo> nearbyActiveReports = baoCaoSuCoRepository.findActiveNearby(
                newIncident.getLoaiSuCo().getLoaiSuCoId(),
                newIncident.getViDo(),
                newIncident.getKinhDo(),
                50.0
        );

        List<BaoCaoSuCo> oldReports = nearbyActiveReports.stream()
                .filter(r -> !r.getBaoCaoId().equals(newIncident.getBaoCaoId()))
                .collect(Collectors.toList());

        if (oldReports != null && !oldReports.isEmpty()) {
            for (BaoCaoSuCo oldReport : oldReports) {
                oldReport.setTrangThai(ReportStatus.AN_HIEN_THI);
                baoCaoSuCoRepository.save(oldReport);
            }

            newIncident.setTrangThai(ReportStatus.DA_XAC_MINH);

            newIncident.setThoiGianBaoCao(LocalDateTime.now());

            return true;
        }
        return false;
    }
}