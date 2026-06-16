package com.traffic.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.dto.response.ReportResponse;
import com.traffic.service.EmailService;
import com.traffic.service.TrafficService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private TrafficService trafficService;

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${MAIL_FROM:Traffic System <onboarding@resend.dev>}")
    private String mailFrom;

    @Value("${APP_BASE_URL:https://traffic-backend-v2.onrender.com}")
    private String baseUrl;

    @Value("${FRONTEND_URL:https://traffic-system-vn.vercel.app}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            if (toEmail == null || toEmail.trim().isEmpty()) {
                System.err.println("[RESEND] Email nhận rỗng, hủy gửi.");
                return;
            }

            Map<String, Object> payload = Map.of(
                    "from", mailFrom,
                    "to", new String[]{toEmail},
                    "subject", subject,
                    "html", htmlContent
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("[RESEND] Status: " + response.statusCode());
            System.out.println("[RESEND] Body: " + response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Resend gửi lỗi: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email bằng Resend thất bại: " + e.getMessage());
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String hoTen, String token) {
        String verifyLink = baseUrl + "/api/auth/verify?token=" + token;

        String htmlContent =
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;'>"
                        + "<div style='background:#2563eb;color:white;padding:20px;text-align:center;'>"
                        + "<h2 style='margin:0;'>XÁC NHẬN ĐĂNG KÝ</h2>"
                        + "</div>"
                        + "<div style='padding:20px;color:#111827;line-height:1.6;'>"
                        + "<p>Chào <b>" + hoTen + "</b>,</p>"
                        + "<p>Vui lòng nhấn nút bên dưới để kích hoạt tài khoản.</p>"
                        + "<div style='text-align:center;margin:30px 0;'>"
                        + "<a href='" + verifyLink + "' style='background:#16a34a;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;'>KÍCH HOẠT TÀI KHOẢN</a>"
                        + "</div>"
                        + "<p style='font-size:12px;color:#6b7280;'>Link có hiệu lực trong 24 giờ.</p>"
                        + "</div>"
                        + "</div>";

        sendHtmlEmail(toEmail, "Kích hoạt tài khoản Traffic System", htmlContent);
    }

    @Override
    @Async
    public void sendOtpPasswordEmail(String toEmail, String otpCode) {
        String htmlContent =
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;'>"
                        + "<div style='background:#f59e0b;color:white;padding:20px;text-align:center;'>"
                        + "<h2 style='margin:0;'>KHÔI PHỤC MẬT KHẨU</h2>"
                        + "</div>"
                        + "<div style='padding:20px;color:#111827;line-height:1.6;'>"
                        + "<p>Mã OTP của bạn là:</p>"
                        + "<div style='text-align:center;margin:25px 0;background:#f9fafb;padding:16px;border:1px dashed #f59e0b;border-radius:6px;'>"
                        + "<span style='font-size:32px;font-weight:bold;letter-spacing:6px;color:#dc2626;'>" + otpCode + "</span>"
                        + "</div>"
                        + "<p>Mã này có hiệu lực trong 5 phút.</p>"
                        + "</div>"
                        + "</div>";

        sendHtmlEmail(toEmail, "Mã OTP khôi phục mật khẩu", htmlContent);
    }

    @Override
    @Async
    public void sendTrafficIncidentAlert(String toEmail, ReportResponse report, String tenDuong) {
        String formattedTime = report.getThoiGianBaoCao() != null
                ? report.getThoiGianBaoCao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                : "Vừa xong";

        String linkWebsite = frontendUrl + "/map?lat=" + report.getViDo() + "&lng=" + report.getKinhDo();

        String htmlContent =
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;'>"
                        + "<div style='background:#dc2626;color:white;padding:20px;text-align:center;'>"
                        + "<h2 style='margin:0;'>CẢNH BÁO SỰ CỐ GIAO THÔNG</h2>"
                        + "</div>"
                        + "<div style='padding:20px;color:#111827;line-height:1.6;'>"
                        + "<p>Xin chào <b>" + (report.getTenDangNhap() != null ? report.getTenDangNhap() : "") + "</b>,</p>"
                        + "<p>Hệ thống ghi nhận cập nhật về báo cáo giao thông của bạn.</p>"
                        + "<table style='width:100%;border-collapse:collapse;margin:20px 0;'>"
                        + "<tr><td style='font-weight:bold;padding:8px 0;'>Loại sự cố:</td><td>" + report.getTenLoaiSuCo() + "</td></tr>"
                        + "<tr><td style='font-weight:bold;padding:8px 0;'>Vị trí:</td><td><a href='" + linkWebsite + "'>" + tenDuong + "</a></td></tr>"
                        + "<tr><td style='font-weight:bold;padding:8px 0;'>Thời gian:</td><td>" + formattedTime + "</td></tr>"
                        + "<tr><td style='font-weight:bold;padding:8px 0;'>Mô tả:</td><td>" + (report.getMoTa() != null ? report.getMoTa() : "Không có mô tả") + "</td></tr>"
                        + "</table>"
                        + "<div style='text-align:center;margin:25px 0;'>"
                        + "<a href='" + linkWebsite + "' style='background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;'>Xem trên hệ thống</a>"
                        + "</div>"
                        + "</div>"
                        + "</div>";

        sendHtmlEmail(toEmail, "Cảnh báo sự cố giao thông", htmlContent);
    }
}