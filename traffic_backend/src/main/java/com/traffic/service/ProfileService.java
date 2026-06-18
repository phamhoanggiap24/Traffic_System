package com.traffic.service;

import com.traffic.common.ApiResponse;
import com.traffic.dto.request.UpdateLocationRequest;
import com.traffic.entity.TaiKhoan;
import com.traffic.dto.request.ChangePasswordRequest;
import com.traffic.dto.request.UpdateProfileRequest;

public interface ProfileService {
    ApiResponse<TaiKhoan> getProfileInfo(String username);
    ApiResponse<String> doiMatKhau(String username, ChangePasswordRequest request);
    ApiResponse<String> updateProfileInfo(String username, UpdateProfileRequest request);
    ApiResponse<String> updateCurrentLocation(String username, UpdateLocationRequest request);
}