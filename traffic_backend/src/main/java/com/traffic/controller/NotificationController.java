package com.traffic.controller;

import com.traffic.dto.response.NotificationResponse;
import com.traffic.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // API lấy danh sách thông báo
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam("taiKhoanId") Long taiKhoanId,
            @RequestParam(value = "vaiTro", defaultValue = "ROLE_USER") String vaiTro) {

        List<NotificationResponse> dtos = notificationService.getBellNotifications(taiKhoanId, vaiTro);
        return ResponseEntity.ok(dtos);
    }

    // API đánh dấu đã đọc
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        boolean updated = notificationService.markAsRead(id);
        if (updated) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // API Đọc tất cả
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestParam("taiKhoanId") Long taiKhoanId,
            @RequestParam(value = "vaiTro", defaultValue = "ROLE_USER") String vaiTro) {
        notificationService.markAllAsRead(taiKhoanId, vaiTro);
        return ResponseEntity.ok().build();
    }

    // API Xóa 1 thông báo
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") Long id) {
        boolean deleted = notificationService.deleteNotification(id);
        return deleted ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // API Xóa tất cả
    @DeleteMapping("/delete-all")
    public ResponseEntity<Void> deleteAllNotifications(
            @RequestParam("taiKhoanId") Long taiKhoanId,
            @RequestParam(value = "vaiTro", defaultValue = "ROLE_USER") String vaiTro) {
        notificationService.deleteAllNotifications(taiKhoanId, vaiTro);
        return ResponseEntity.ok().build();
    }
}