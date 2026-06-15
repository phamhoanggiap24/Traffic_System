package com.traffic.service.impl;

import com.traffic.dto.response.ReportResponse;
import com.traffic.service.EmailService;
import com.traffic.service.TrafficService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TrafficService trafficService;

    @Value("${spring.mail.username:${MAIL_USERNAME:your-email@gmail.com}}")
    private String emailSender;

    @Value("${APP_BASE_URL:http://localhost:8080}") // Tự động nhận diện URL hệ thống khi deploy
    private String baseUrl;

    // =========================================================================
    // 1. LUỒNG GỬI MAIL CẢNH BÁO SỰ CỐ GIAO THÔNG (TỰ ĐỘNG DUYỆT & DUYỆT THỦ CÔNG)
    // =========================================================================
    @Override
    @Async
    public void sendTrafficIncidentAlert(String toEmail, ReportResponse report, String tenDuong) {
        System.out.println("[ASYNC-MAIL] >>> Nhận tác vụ gửi mail cảnh báo sự cố ngầm thành công!");

        if (toEmail == null || toEmail.trim().isEmpty() || toEmail.contains("your-email")) {
            System.err.println("[ASYNC-MAIL] Địa chỉ email nhận không hợp lệ! Hủy tác vụ gửi.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailSender, "Hệ Thống Giám Sát Giao Thông");
            helper.setTo(toEmail);
            helper.setSubject("CẢNH BÁO: " + report.getTenLoaiSuCo().toUpperCase() + " TẠI KHU VỰC CỦA BẠN");

            String formattedTime = report.getThoiGianBaoCao() != null
                    ? report.getThoiGianBaoCao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
                    : "Vừa xong";

            // Sửa lại thành link map của website (có thể đổi localhost thành domain frontend nếu cần)
            String linkWebsite = "http://localhost:3000/map?lat=" + report.getViDo() + "&lng=" + report.getKinhDo();

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>"
                    + "  <div style='background-color: #d9534f; color: white; padding: 20px; text-align: center;'>"
                    + "    <h2 style='margin: 0;'>CẢNH BÁO SỰ CỐ GIAO THÔNG</h2>"
                    + "  </div>"
                    + "  <div style='padding: 20px; color: #333333; line-height: 1.6;'>"
                    + "    <p>Xin chào <b>" + (report.getTenDangNhap() != null ? report.getTenDangNhap() : "") + "</b>,</p>"
                    + "    <p>Hệ thống giám sát vừa xác minh một sự cố giao thông mới dựa trên dữ liệu bạn đóng góp hoặc từ thực tế khu vực.</p>"
                    + "    <table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>"
                    + "      <tr><td style='padding: 8px 0; font-weight: bold; width: 35%;'>Loại sự cố:</td><td style='padding: 8px 0; color: #d9534f; font-weight: bold;'>" + report.getTenLoaiSuCo() + "</td></tr>"
                    + "      <tr>"
                    + "        <td style='padding: 8px 0; font-weight: bold;'>Địa chỉ/Vị trí:</td>"
                    + "        <td style='padding: 8px 0;'>"
                    + "          <a href='" + linkWebsite + "' target='_blank' style='color: #0275d8; font-weight: bold; text-decoration: underline;'>" + tenDuong + "</a>"
                    + "        </td>"
                    + "      </tr>"
                    + "      <tr><td style='padding: 8px 0; font-weight: bold;'>Thời gian báo:</td><td style='padding: 8px 0;'>" + formattedTime + "</td></tr>"
                    + "      <tr><td style='padding: 8px 0; font-weight: bold;'>Mô tả chi tiết:</td><td style='padding: 8px 0;'>" + (report.getMoTa() != null ? report.getMoTa() : "Không có mô tả") + "</td></tr>"
                    + "    </table>"
                    + "    "
                    + "    <div style='text-align: center; margin: 25px 0 15px 0;'>"
                    + "       <a href='" + linkWebsite + "' target='_blank' style='background-color: #0275d8; color: white; padding: 12px 24px; text-decoration: none; font-weight: bold; border-radius: 4px; display: inline-block; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>Xem Chi Tiết Trên Hệ Thống</a>"
                    + "    </div>"
                    + "    "
                    + "    <p style='background-color: #f7f7f7; padding: 10px; border-left: 4px solid #d9534f; font-style: italic; margin-top: 20px;'>"
                    + "       Vui lòng chủ động kiểm tra lộ trình di chuyển khác thích hợp để tránh gặp ùn tắc cục bộ."
                    + "    </p>"
                    + "  </div>"
                    + "  <div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0;'>"
                    + "    Email tự động từ Hệ thống Giám sát giao thông. Vui lòng không phản hồi thư này."
                    + "  </div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("[EmailService] ĐÃ GỬI MAIL SỰ CỐ THÀNH CÔNG TỚI: " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Lỗi gửi mail sự cố: " + e.getMessage());
        }
    }

    // 2. LUỒNG GỬI MAIL KÍCH HOẠT TÀI KHOẢN KHI ĐĂNG KÝ
    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String hoTen, String token) {
        System.out.println("[ASYNC-MAIL] >>> Nhận tác vụ gửi mail kích hoạt tài khoản ngầm...");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String verifyLink = baseUrl + "/api/auth/verify?token=" + token;

            helper.setFrom(emailSender, "Hệ Thống Giám Sát Giao Thông");
            helper.setTo(toEmail);
            helper.setSubject("🔥 KÍCH HOẠT TÀI KHOẢN HỆ THỐNG GIAO THÔNG THÔNG MINH");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>"
                    + "  <div style='background-color: #0275d8; color: white; padding: 20px; text-align: center;'>"
                    + "    <h2 style='margin: 0;'>XÁC NHẬN ĐĂNG KÝ THÀNH VIÊN</h2>"
                    + "  </div>"
                    + "  <div style='padding: 20px; color: #333333; line-height: 1.6;'>"
                    + "    <p>Chào mừng <b>" + hoTen + "</b> đã đến với hệ thống,</p>"
                    + "    <p>Cảm ơn bạn đã tham gia cộng đồng hỗ trợ giám sát và điều tiết giao thông đô thị. Vui lòng nhấn vào nút bên dưới để xác thực email và kích hoạt tài khoản sử dụng bản đồ:</p>"
                    + "    <div style='text-align: center; margin: 30px 0;'>"
                    + "      <a href='" + verifyLink + "' target='_blank' style='background-color: #28a745; color: white; padding: 12px 25px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>KÍCH HOẠT TÀI KHOẢN NGAY</a>"
                    + "    </div>"
                    + "    <p style='color: #777777; font-size: 12px; background-color: #f9f9f9; padding: 10px; border-radius: 4px;'>"
                    + "       ⚠️ <b>Lưu ý an toàn:</b> Đường liên kết xác thực này chỉ có hiệu lực trong vòng <b>24 giờ</b> kể từ thời điểm đăng ký."
                    + "    </p>"
                    + "  </div>"
                    + "  <div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0;'>"
                    + "    Email tự động từ Hệ thống Giám sát giao thông. Vui lòng không phản hồi thư này."
                    + "  </div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("[EmailService] ĐÃ GỬI MAIL KÍCH HOẠT THÀNH CÔNG TỚI: " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Lỗi gửi email kích hoạt tài khoản: " + e.getMessage());
        }
    }

    // 3. LUỒNG GỬI MÃ OTP QUÊN MẬT KHẨU
    @Override
    @Async
    public void sendOtpPasswordEmail(String toEmail, String otpCode) {
        System.out.println("[ASYNC-MAIL] >>> Nhận tác vụ gửi mã OTP khôi phục mật khẩu ngầm...");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailSender, "Hệ Thống Giám Sát Giao Thông");
            helper.setTo(toEmail);
            helper.setSubject("🔒 MÃ XÁC THỰC KHÔI PHỤC MẬT KHẨU");

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>"
                    + "  <div style='background-color: #f0ad4e; color: white; padding: 20px; text-align: center;'>"
                    + "    <h2 style='margin: 0;'>KHÔI PHỤC MẬT KHẨU</h2>"
                    + "  </div>"
                    + "  <div style='padding: 20px; color: #333333; line-height: 1.6;'>"
                    + "    <p>Xin chào,</p>"
                    + "    <p>Hệ thống nhận được yêu cầu cấp lại mật khẩu từ tài khoản gắn liền với email này. Mã xác thực OTP dùng một lần (OTP) của bạn là:</p>"
                    + "    <div style='text-align: center; margin: 25px 0; background-color: #f8f9fa; padding: 15px; border-radius: 4px; border: 1px dashed #f0ad4e;'>"
                    + "      <span style='font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #d9534f;'>" + otpCode + "</span>"
                    + "    </div>"
                    + "    <p>Mã xác thực này chỉ có hiệu lực trong vòng <b>5 phút</b>. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua thư hoặc tiến hành kiểm tra lại bảo mật tài khoản.</p>"
                    + "  </div>"
                    + "  <div style='background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 12px; color: #777777; border-top: 1px solid #e0e0e0;'>"
                    + "    Email tự động từ Hệ thống Giám sát giao thông. Vui lòng không phản hồi thư này."
                    + "  </div>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            System.out.println("[EmailService] ĐÃ GỬI MAIL OTP THÀNH CÔNG TỚI: " + toEmail);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage());
        }
    }
}