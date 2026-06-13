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

        // Đọc trực tiếp từ Biến môi trường của Render, nếu thiếu sẽ lấy mặc định phía sau
        String host = System.getenv("SPRING_MAIL_HOST") != null ? System.getenv("SPRING_MAIL_HOST") : "smtp.gmail.com";
        int port = System.getenv("SPRING_MAIL_PORT") != null ? Integer.parseInt(System.getenv("SPRING_MAIL_PORT")) : 587;
        String username = System.getenv("SPRING_MAIL_USERNAME") != null ? System.getenv("SPRING_MAIL_USERNAME") : "your-email@gmail.com";
        String password = System.getenv("SPRING_MAIL_PASSWORD") != null ? System.getenv("SPRING_MAIL_PASSWORD") : "your-password";

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        return mailSender;
    }
}