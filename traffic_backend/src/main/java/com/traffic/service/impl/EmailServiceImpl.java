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

    @Value("${MAIL_USERNAME}")
    private String emailSender;

    @Override
    @Async
    public void sendTrafficIncidentAlert(String toEmail, ReportResponse report, String tenDuong) {
        System.out.println("[ASYNC-MAIL] >>> Nhận tác vụ gửi mail ngầm thành công!");
        System.out.println("[ASYNC-MAIL] Email gửi đi (From): " + emailSender);
        System.out.println("[ASYNC-MAIL] Email nhận (To): " + toEmail);
        System.out.println("[ASYNC-MAIL] Vị trí sự cố: " + tenDuong);

        // Kiểm tra xem email nhận có hợp lệ không (tránh gửi vào email rác hoặc chuỗi trống)
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

            String linkWebsite = "http://localhost:3000/map?lat=" + report.getViDo() + "&lng=" + report.getKinhDo();

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden;'>"
                    + "  <div style='background-color: #d9534f; color: white; padding: 20px; text-align: center;'>"
                    + "    <h2 style='margin: 0;'>CẢNH BÁO SỰ CỐ GIAO THÔNG</h2>"
                    + "  </div>"
                    + "  <div style='padding: 20px; color: #333333; line-height: 1.6;'>"
                    + "    <p>Xin chào,</p>"
                    + "    <p>Hệ thống giám sát vừa xác minh một sự cố giao thông mới tại vị trí bạn đã báo cáo.</p>"
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
            System.out.println("[EmailService] ĐÃ GỬI MAIL THÀNH CÔNG TỚI: " + toEmail);

        } catch (Exception e) {
            System.err.println("[EmailService] Lỗi nghiêm trọng khi thực hiện gửi mail: " + e.getMessage());
            e.printStackTrace();
        }
    }
}