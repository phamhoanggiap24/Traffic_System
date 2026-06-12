package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.UpdateProfileRequest;
import com.traffic.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @PutMapping("/update-info")
    public ResponseEntity<ApiResponse<String>> updateInfo(@RequestBody UpdateProfileRequest request) {
        String username = request.getTenDangNhap();
        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(400).body(new ApiResponse<>(400, "Thiếu thông tin tài khoản", null));
        }

        ApiResponse<String> response = profileService.updateProfileInfo(username, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // API Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> doiMatKhau(@RequestBody ChangePasswordRequest request) {
        String username = request.getTenDangNhap();
        if (username == null || username.isEmpty()) {
            return ResponseEntity.status(400).body(new ApiResponse<>(400, "Thiếu thông tin tài khoản", null));
        }

        ApiResponse<String> response = profileService.doiMatKhau(username, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}