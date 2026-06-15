package com.traffic.service;

import com.traffic.dto.response.ReportResponse;

public interface EmailService {
    void sendTrafficIncidentAlert(String toEmail, ReportResponse report, String tenDuong);
    void sendVerificationEmail(String toEmail, String hoTen, String token);
    void sendOtpPasswordEmail(String toEmail, String otpCode);
}