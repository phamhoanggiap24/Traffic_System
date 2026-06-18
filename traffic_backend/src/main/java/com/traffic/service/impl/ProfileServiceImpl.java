package com.traffic.service.impl;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.UpdateProfileRequest;
import com.traffic.entity.TaiKhoan;
import com.traffic.dto.request.UpdateLocationRequest;
import com.traffic.entity.TuyChonCaNhan;
import com.traffic.repository.TuyChonCaNhanRepository;
import java.time.LocalDateTime;
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

    @Autowired
    private TuyChonCaNhanRepository tuyChonCaNhanRepository;

    @Override
    public ApiResponse<TaiKhoan> getProfileInfo(String username) {
        try {
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            // Xóa mật khẩu trước khi gửi về Client để đảm bảo bảo mật
            tk.setMatKhau(null);

            return new ApiResponse<>(200, "Lấy thông tin tài khoản thành công!", tk);
        } catch (Exception e) {
            return new ApiResponse<>(500, "Lỗi: " + e.getMessage(), null);
        }
    }

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
            tk.setSoDienThoai(request.getSoDienThoai());

            taiKhoanRepository.save(tk);
            return new ApiResponse<>(200, "Cập nhật thông tin thành công!", "OK");
        } catch (Exception e) {
            return new ApiResponse<>(500, "Lỗi khi cập nhật: " + e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse<String> updateCurrentLocation(String username, UpdateLocationRequest request) {
        try {
            TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            TuyChonCaNhan option = tuyChonCaNhanRepository
                    .findByTaiKhoanTaiKhoanId(tk.getTaiKhoanId())
                    .orElseGet(() -> {
                        TuyChonCaNhan t = new TuyChonCaNhan();
                        t.setTaiKhoan(tk);
                        t.setNhanThongBao(true);
                        t.setBanKinhCanhBao(100f);
                        return t;
                    });

            option.setViDoHienTai(request.getViDo());
            option.setKinhDoHienTai(request.getKinhDo());
            option.setThoiGianCapNhatViTri(LocalDateTime.now());

            tuyChonCaNhanRepository.save(option);

            return new ApiResponse<>(200, "Cập nhật vị trí thành công", "OK");
        } catch (Exception e) {
            return new ApiResponse<>(500, "Lỗi cập nhật vị trí: " + e.getMessage(), null);
        }
    }
}