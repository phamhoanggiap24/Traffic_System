package com.traffic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

@Configuration
public class MailConfig {

    // Ưu tiên đọc từ biến môi trường Render, nếu không có sẽ lấy giá trị mặc định ở sau dấu hai chấm (:)
    @Value("${MAIL_HOST:smtp.gmail.com}")
    private String host;

    @Value("${MAIL_PORT:587}")
    private int port;

    @Value("${MAIL_USERNAME:your-email@gmail.com}")
    private String username;

    @Value("${MAIL_PASSWORD:your-password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        // Bật true để khi deploy lên Render, nếu gửi lỗi nó sẽ in chi tiết log SMTP ra mục Logs cho bạn xem
        props.put("mail.debug", "true");

        return mailSender;
    }
}