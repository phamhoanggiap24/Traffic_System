package com.traffic.service;

import com.traffic.dto.request.ReportRequest;
import com.traffic.dto.response.ReportResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IncidentReportService {
    void verifyReport(Long id, String trangThaiMoi);
    ReportResponse processReportSubmission(ReportRequest request);
    Page<ReportResponse> getFilteredReportsPage(Integer loaiSuCoId, String tenDangNhap, String trangThai, String ngay, Pageable pageable);
    void deleteReport(Long id);
    void removeReportFromMap(Long id);
    List<ReportResponse> getPublicMapReports();
    long getPendingReportsCount();
}