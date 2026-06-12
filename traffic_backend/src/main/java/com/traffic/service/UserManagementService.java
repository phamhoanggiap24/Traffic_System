package com.traffic.service;

import com.traffic.dto.response.UserManagementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserManagementService {
    Page<UserManagementResponse> getAllUsers(String tenDangNhap, Pageable pageable);
    void deleteUser(Long id);
    void lockUser(Long id);
    void unlockUser(Long id);
}