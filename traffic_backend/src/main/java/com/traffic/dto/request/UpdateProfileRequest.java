package com.traffic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String tenDangNhap;

    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @Email(message = "Email không đúng định dạng")
    private String email;

    private String soDienThoai;
}