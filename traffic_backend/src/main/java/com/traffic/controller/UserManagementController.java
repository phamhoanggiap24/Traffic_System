package com.traffic.controller;

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

    // LẤY DANH SÁCH USER PHÂN TRANG KÈM TÌM KIẾM
    @GetMapping("/users")
    public ResponseEntity<Page<UserManagementResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size,
            @RequestParam(required = false) String tenDangNhap) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("doTinCayNguoiDung").ascending());

        return ResponseEntity.ok(userManagementService.getAllUsers(tenDangNhap, pageable));
    }

    // XÓA TÀI KHOẢN NGƯỜI DÙNG
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userManagementService.deleteUser(id);
        return ResponseEntity.ok("Đã xóa tài khoản thành công!");
    }

    // KHÓA TÀI KHOẢN NGƯỜI DÙNG
    @PatchMapping("/users/lock/{id}")
    public ResponseEntity<String> lockUser(@PathVariable Long id) {
        userManagementService.lockUser(id);
        return ResponseEntity.ok("Đã khóa tài khoản thành công!");
    }

    // KHÔI PHỤC TÀI KHOẢN NGƯỜI DÙNG
    @PatchMapping("/users/unlock/{id}")
    public ResponseEntity<String> unlockUser(@PathVariable Long id) {
        userManagementService.unlockUser(id);
        return ResponseEntity.ok("Đã khôi phục tài khoản thành công!");
    }
}