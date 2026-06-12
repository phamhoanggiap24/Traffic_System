package com.traffic.common;

import lombok.Getter;

@Getter
public enum AuthMessage {
    OTP_SENT("Mã OTP đã được gửi đến Gmail của bạn!"),
    OTP_INVALID("Mã OTP không chính xác, vui lòng kiểm tra lại."),
    OTP_EXPIRED("Mã OTP đã hết hạn sử dụng."),
    PASSWORD_RESET_SUCCESS("Đặt lại mật khẩu thành công!"),
    EMAIL_NOT_FOUND("Email không tồn tại trong hệ thống."),
    PASSWORD_CHANGE_SUCCESS("Đổi mật khẩu thành công!");

    private final String message;

    AuthMessage(String message) {
        this.message = message;
    }
}