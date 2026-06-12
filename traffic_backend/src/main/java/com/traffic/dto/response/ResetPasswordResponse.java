package com.traffic.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ResetPasswordResponse {
    private String email;
    private LocalDateTime thoiGianHetHan;
    private String message;
    private boolean success;
}