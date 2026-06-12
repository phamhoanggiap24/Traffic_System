package com.traffic.dto.request;

import lombok.Data;

@Data
public class NotificationRequest {
    private String noiDung;
    private String loaiCanhBao;
    private Long taiKhoanId;
    private Long baoCaoId;
}