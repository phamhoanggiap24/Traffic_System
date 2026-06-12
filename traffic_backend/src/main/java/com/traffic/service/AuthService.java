package com.traffic.service;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.LoginRequest;
import com.traffic.dto.request.RegisterRequest;
import com.traffic.dto.request.ResetPasswordRequest;
import com.traffic.dto.response.AuthResponse;
import com.traffic.dto.response.ResetPasswordResponse;
import com.traffic.dto.response.UserManagementResponse;

public interface AuthService {
    ApiResponse<UserManagementResponse> register(RegisterRequest request);

    ApiResponse<AuthResponse> login(LoginRequest request);

    ApiResponse<AuthResponse> changePassword(ChangePasswordRequest request);

    ApiResponse<ResetPasswordResponse> forgotPassword(String email);

    ApiResponse<Void> logout(String token);

    ApiResponse<UserManagementResponse> createAdminAccount(RegisterRequest request);

    ApiResponse<String> resetPassword(ResetPasswordRequest request);

    int verifyAccount(String token);

    ApiResponse<AuthResponse> refreshToken(String refreshToken);
}