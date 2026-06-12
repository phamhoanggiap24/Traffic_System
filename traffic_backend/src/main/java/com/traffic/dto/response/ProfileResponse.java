package com.traffic.dto.response;

import lombok.Data;

@Data
public class ProfileResponse {
    private Long taiKhoanId;
    private String tenDangNhap;
    private String hoTen;
    private String email;
    private String soDienThoai;
    private String vaiTro;
}