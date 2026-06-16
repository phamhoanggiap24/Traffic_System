package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.*;
import com.traffic.dto.response.AuthResponse;
import com.traffic.dto.response.ResetPasswordResponse;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${FRONTEND_URL:https://traffic-system-vn.vercel.app}")
    private String frontendUrl;

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

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        int result = authService.verifyAccount(token);

        String title;
        String message;
        String icon;
        String color;
        String buttonText;
        String redirectUrl = frontendUrl + "/login";

        if (result == 1) {
            title = "Kích hoạt tài khoản thành công";
            message = "Tài khoản của bạn đã được xác thực. Bạn có thể đăng nhập và sử dụng hệ thống Traffic Map ngay bây giờ.";
            icon = "✓";
            color = "#16a34a";
            buttonText = "Đăng nhập ngay";
        } else if (result == 0) {
            title = "Liên kết xác thực đã hết hạn";
            message = "Link kích hoạt đã quá thời hạn sử dụng. Vui lòng đăng ký lại hoặc yêu cầu gửi lại email xác thực mới.";
            icon = "!";
            color = "#f59e0b";
            buttonText = "Quay về đăng nhập";
        } else {
            title = "Xác thực không hợp lệ";
            message = "Mã xác thực không tồn tại hoặc đã được sử dụng. Vui lòng kiểm tra lại email hoặc thực hiện đăng ký lại.";
            icon = "×";
            color = "#ef4444";
            buttonText = "Quay về đăng nhập";
        }

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                    <meta http-equiv="refresh" content="5;url=%s" />
                    <title>%s</title>
                    <style>
                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            min-height: 100vh;
                            font-family: Arial, Helvetica, sans-serif;
                            background: linear-gradient(135deg, #e0f2fe 0%%, #eef2ff 45%%, #f8fafc 100%%);
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 24px;
                            color: #0f172a;
                        }

                        .card {
                            width: 100%%;
                            max-width: 560px;
                            background: #ffffff;
                            border-radius: 20px;
                            padding: 36px 28px;
                            text-align: center;
                            box-shadow: 0 20px 60px rgba(15, 23, 42, 0.16);
                            border: 1px solid #e2e8f0;
                        }

                        .brand {
                            font-size: 14px;
                            font-weight: 700;
                            letter-spacing: 2px;
                            text-transform: uppercase;
                            color: #2563eb;
                            margin-bottom: 18px;
                        }

                        .icon {
                            width: 84px;
                            height: 84px;
                            border-radius: 50%%;
                            margin: 0 auto 22px;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 46px;
                            font-weight: 800;
                            color: white;
                            background: %s;
                            box-shadow: 0 12px 30px rgba(15, 23, 42, 0.18);
                        }

                        h1 {
                            margin: 0 0 14px;
                            font-size: 28px;
                            line-height: 1.25;
                            color: #0f172a;
                        }

                        p {
                            margin: 0 auto;
                            max-width: 440px;
                            font-size: 16px;
                            line-height: 1.65;
                            color: #475569;
                        }

                        .note {
                            margin-top: 18px;
                            font-size: 14px;
                            color: #64748b;
                        }

                        .btn {
                            display: inline-block;
                            margin-top: 28px;
                            background: #2563eb;
                            color: #ffffff;
                            text-decoration: none;
                            padding: 13px 26px;
                            border-radius: 999px;
                            font-weight: 700;
                            box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
                        }

                        .footer {
                            margin-top: 28px;
                            padding-top: 18px;
                            border-top: 1px solid #e2e8f0;
                            font-size: 13px;
                            color: #94a3b8;
                        }

                        @media (max-width: 480px) {
                            .card {
                                padding: 30px 20px;
                                border-radius: 16px;
                            }

                            h1 {
                                font-size: 24px;
                            }

                            p {
                                font-size: 15px;
                            }

                            .icon {
                                width: 74px;
                                height: 74px;
                                font-size: 40px;
                            }
                        }
                    </style>
                </head>
                <body>
                    <main class="card">
                        <div class="brand">Traffic Map System</div>
                        <div class="icon">%s</div>
                        <h1>%s</h1>
                        <p>%s</p>
                        <div class="note">Hệ thống sẽ tự động chuyển về trang đăng nhập sau 5 giây.</div>
                        <a class="btn" href="%s">%s</a>
                        <div class="footer">Cảm ơn bạn đã sử dụng hệ thống cảnh báo giao thông trực tuyến.</div>
                    </main>
                </body>
                </html>
                """.formatted(
                redirectUrl,
                title,
                color,
                icon,
                title,
                message,
                redirectUrl,
                buttonText
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
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