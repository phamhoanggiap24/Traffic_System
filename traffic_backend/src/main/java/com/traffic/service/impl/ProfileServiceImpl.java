package com.traffic.service.impl;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.UpdateProfileRequest;
import com.traffic.entity.TaiKhoan;
import com.traffic.repository.TaiKhoanRepository;
import com.traffic.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public ApiResponse<String> doiMatKhau(String username, ChangePasswordRequest request) {
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getMatKhauCu(), tk.getMatKhau())) {
            return new ApiResponse<>(400, "Mật khẩu cũ không chính xác!", null);
        }

        // Cập nhật mật khẩu mới
        tk.setMatKhau(passwordEncoder.encode(request.getMatKhauMoi()));
        taiKhoanRepository.save(tk);

        return new ApiResponse<>(200, "Thay đổi mật khẩu thành công!", "Success");
    }

    @Override
    public ApiResponse<String> updateProfileInfo(String username, UpdateProfileRequest request) {
        try {
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            tk.setHoTen(request.getHoTen());
            tk.setEmail(request.getEmail());
            tk.setSoDienThoai(request.getSoDienThoai());

            taiKhoanRepository.save(tk);
            return new ApiResponse<>(200, "Cập nhật thông tin thành công!", "OK");
        } catch (Exception e) {
            return new ApiResponse<>(500, "Lỗi khi cập nhật: " + e.getMessage(), null);
        }
    }
}