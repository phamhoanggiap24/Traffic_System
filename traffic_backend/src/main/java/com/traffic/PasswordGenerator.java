package com.traffic;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    @Test
    void generatePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "09112004@Giap";
        String encodedPassword = encoder.encode(rawPassword);

        System.out.println("KẾT QUẢ MÃ HÓA:");
        System.out.println(encodedPassword);
    }
}