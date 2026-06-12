package com.traffic.dto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String tenDangNhap;
    private String matKhauCu;
    private String matKhauMoi;
}