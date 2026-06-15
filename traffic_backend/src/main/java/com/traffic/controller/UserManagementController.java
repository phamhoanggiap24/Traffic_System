package com.traffic.controller;

import com.traffic.common.ApiResponse;
import com.traffic.dto.response.UserManagementResponse;
import com.traffic.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class UserManagementController {

    @Autowired
    private UserManagementService userManagementService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserManagementResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size,
            @RequestParam(required = false) String tenDangNhap) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("doTinCayNguoiDung").ascending());
        var data = userManagementService.getAllUsers(tenDangNhap, pageable);

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thành công", data));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa tài khoản thành công!", null));
    }

    @PatchMapping("/users/lock/{id}")
    public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable Long id) {
        userManagementService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã khóa tài khoản thành công!", null));
    }

    @PatchMapping("/users/unlock/{id}")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable Long id) {
        userManagementService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã khôi phục tài khoản thành công!", null));
    }
}