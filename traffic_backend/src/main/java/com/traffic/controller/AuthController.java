package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.*;
import com.traffic.dto.response.AuthResponse;
import com.traffic.dto.response.ResetPasswordResponse;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserManagementResponse>> createAdmin(@RequestBody RegisterRequest request) {
        ApiResponse<UserManagementResponse> response = authService.createAdminAccount(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserManagementResponse>> register(@RequestBody RegisterRequest request) {
        ApiResponse<UserManagementResponse> response = authService.register(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token) {
        int result = authService.verifyAccount(token);

        if (result == 1) {
            return "<h1>Kích hoạt thành công!</h1><p>Chào mừng bạn đến với hệ thống.</p>";
        } else if (result == 0) {
            return "<h1>Link đã hết hạn!</h1><p>Vui lòng thực hiện đăng ký lại hoặc yêu cầu gửi lại link mới.</p>";
        } else {
            return "<h1>Lỗi!</h1><p>Mã xác thực không tồn tại hoặc không hợp lệ.</p>";
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        ApiResponse<AuthResponse> response = authService.login(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ApiResponse<ResetPasswordResponse> response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse<String> response = authService.resetPassword(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<AuthResponse>> changePassword(@RequestBody ChangePasswordRequest request) {
        ApiResponse<AuthResponse> response = authService.changePassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        ApiResponse<Void> response = authService.logout(token);
        return ResponseEntity.ok(response);
    }
}