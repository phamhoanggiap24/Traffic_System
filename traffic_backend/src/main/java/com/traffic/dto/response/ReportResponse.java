package com.traffic.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.traffic.common.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {
    private Long baoCaoId;
    private String moTa;
    private Double viDo;
    private Double kinhDo;
    private String hinhAnhUrl;
    private ReportStatus trangThai;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime thoiGianBaoCao;
    private String tenLoaiSuCo;
    private String tenDangNhap;
}