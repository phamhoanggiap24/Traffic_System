package com.traffic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Đọc biến từ môi trường Render (nếu có), nếu rỗng sẽ lấy giá trị mặc định của bạn
        String host = System.getenv("MAIL_HOST") != null ? System.getenv("MAIL_HOST") : "smtp.gmail.com";
        String portStr = System.getenv("MAIL_PORT") != null ? System.getenv("MAIL_PORT") : "587";
        String username = System.getenv("MAIL_USERNAME") != null ? System.getenv("MAIL_USERNAME") : "your-email@gmail.com";
        String password = System.getenv("MAIL_PASSWORD") != null ? System.getenv("MAIL_PASSWORD") : "your-password";

        mailSender.setHost(host);
        mailSender.setPort(Integer.parseInt(portStr));
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}