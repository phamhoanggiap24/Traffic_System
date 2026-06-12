package com.traffic.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.Size;

@Data
public class ReportRequest {
    @Size(max = 200, message = "Mô tả sự cố không được vượt quá 200 ký tự!")
    private String moTa;

    private Double viDo;
    private Double kinhDo;

    private String hinhAnhUrl;

    private MultipartFile hinhAnh;
    private Integer loaiSuCoId;
    private Long taiKhoanId;
}