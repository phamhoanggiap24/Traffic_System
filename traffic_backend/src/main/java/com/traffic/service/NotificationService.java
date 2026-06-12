package com.traffic.service;

import com.traffic.dto.response.NotificationResponse;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getBellNotifications(Long taiKhoanId, String vaiTro);
    boolean markAsRead(Long canhBaoId);
    void markAllAsRead(Long taiKhoanId, String vaiTro);
    boolean deleteNotification(Long canhBaoId);
    void deleteAllNotifications(Long taiKhoanId, String vaiTro);
}