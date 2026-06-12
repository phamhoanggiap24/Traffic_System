package com.traffic.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.traffic.common.UserStatus;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserManagementResponse {
    private Long taiKhoanId;
    private String tenDangNhap;
    private String email;
    private String hoTen;
    private String soDienThoai;
    private Integer doTinCayNguoiDung;
    private List<String> vaiTro;
    private UserStatus trangThaiTaiKhoan;
}