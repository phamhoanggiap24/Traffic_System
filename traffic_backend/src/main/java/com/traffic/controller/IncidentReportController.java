package com.traffic.controller;

import com.traffic.dto.request.ReportRequest;
import com.traffic.common.ApiResponse;
import com.traffic.dto.response.ReportResponse;
import com.traffic.service.IncidentReportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class IncidentReportController {

    @Autowired
    private IncidentReportService baoCaoSuCoService;

    // NGƯỜI DÙNG GỬI BÁO CÁO SỰ CỐ MỚI
    @PostMapping(value = "/gui", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportResponse>> guiBaoCao(@Valid @ModelAttribute ReportRequest request) {
        ReportResponse response = baoCaoSuCoService.processReportSubmission(request);
        return ResponseEntity.ok(new ApiResponse<>(200, "Gửi báo cáo thành công", response));
    }

    // LẤY DANH SÁCH MARKERS HIỂN THỊ CÔNG KHAI TRÊN BẢN ĐỒ
    @GetMapping("/public/markers")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> layMarkersChoBanDo() {
        List<ReportResponse> markers = baoCaoSuCoService.getPublicMapReports();
        return ResponseEntity.ok(new ApiResponse<>(200, "Thành công", markers));
    }

    // LẤY DANH SÁCH PHÂN TRANG
    @GetMapping("/admin/danh-sach")
    public ResponseEntity<ApiResponse<Page<ReportResponse>>> layDanhSachChoAdmin(
            @RequestParam(required = false) Integer loaiSuCoId,
            @RequestParam(required = false) String tenDangNhap,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String ngay,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {

        int pageSize = (size != null && size > 0) ? size : 8;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("thoiGianBaoCao").descending());

        Page<ReportResponse> reports = baoCaoSuCoService.getFilteredReportsPage(
                loaiSuCoId, tenDangNhap, trangThai, ngay, pageable);

        return ResponseEntity.ok(new ApiResponse<>(200, "Thành công", reports));
    }

    // ADMIN PHÊ DUYỆT HOẶC TỪ CHỐI
    @PatchMapping("/admin/xac-minh/{id}")
    public ResponseEntity<ApiResponse<String>> xacMinhBaoCao(
            @PathVariable Long id,
            @RequestParam String trangThaiMoi) {

        baoCaoSuCoService.verifyReport(id, trangThaiMoi);
        return ResponseEntity.ok(new ApiResponse<>(200, "Đã cập nhật trạng thái xác minh thành công.", null));
    }

    // GỠ BÁO CÁO KHỎI BẢN ĐỒ HIỂN THỊ CÔNG KHAI
    @PatchMapping("/admin/go-khoi-map/{id}")
    public ResponseEntity<ApiResponse<String>> goBaoCaoTrenMap(@PathVariable Long id) {
        baoCaoSuCoService.removeReportFromMap(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "Đã gỡ báo cáo khỏi bản đồ hiển thị công khai thành công!", "OK"));
    }

    // XÓA BÁO CÁO TRONG DANH SÁCH QUẢN TRỊ ADMIN
    @DeleteMapping("/admin/xoa/{id}")
    public ResponseEntity<ApiResponse<String>> xoaBaoCao(@PathVariable Long id) {
        baoCaoSuCoService.deleteReport(id);
        return ResponseEntity.ok(new ApiResponse<>(200, "Xóa báo cáo thành công", "OK"));
    }

    // LẤY SỐ LƯỢNG BÁO CÁO CHỜ DUYỆT TẠI THANH ADMIN
    @GetMapping("/admin/pending-count")
    public ResponseEntity<ApiResponse<Long>> laySoLuongBaoCaoChoDuyet() {
        long count = baoCaoSuCoService.getPendingReportsCount();
        return ResponseEntity.ok(new ApiResponse<>(200, "Lấy số lượng báo cáo chờ duyệt thành công", count));
    }
}