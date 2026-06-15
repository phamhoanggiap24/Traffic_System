package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.UpdateProfileRequest;
import com.traffic.entity.TaiKhoan;
import com.traffic.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // 🚀 1. BỔ SUNG ĐƯỜNG DẪN: Lấy thông tin cá nhân Real-time phục vụ React hiển thị điểm uy tín
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TaiKhoan>> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(401, "Chưa đăng nhập hoặc token không hợp lệ", null));
        }

        String currentUsername;

        Object principal = authentication.getPrincipal();

        if (principal instanceof TaiKhoan taiKhoan) {
            currentUsername = taiKhoan.getTenDangNhap();
        } else {
            currentUsername = authentication.getName();
        }

        ApiResponse<TaiKhoan> response = profileService.getProfileInfo(currentUsername);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 🔒 2. ĐÃ BẢO MẬT LẠI: Cập nhật thông tin cá nhân
    @PutMapping("/update-info")
    public ResponseEntity<ApiResponse<String>> updateInfo(@RequestBody UpdateProfileRequest request) {
        // Bảo mật: Lấy từ Token chứ không lấy qua request.getTenDangNhap() nữa
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (username == null || username.isEmpty() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(400).body(new ApiResponse<>(400, "Thiếu thông tin tài khoản hoặc chưa đăng nhập", null));
        }

        ApiResponse<String> response = profileService.updateProfileInfo(username, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // 🔒 3. ĐÃ BẢO MẬT LẠI: API Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> doiMatKhau(@RequestBody ChangePasswordRequest request) {
        // Bảo mật: Lấy từ Token để chặn hành vi đổi trộm mật khẩu người khác
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (username == null || username.isEmpty() || "anonymousUser".equals(username)) {
            return ResponseEntity.status(400).body(new ApiResponse<>(400, "Thiếu thông tin tài khoản hoặc chưa đăng nhập", null));
        }

        ApiResponse<String> response = profileService.doiMatKhau(username, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}