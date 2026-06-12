package com.traffic.dto.response;

import com.traffic.common.ReadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long canhBaoId;
    private String noiDung;
    private String loaiCanhBao;
    private String kenhGui;
    private ReadStatus trangThai;
    private LocalDateTime thoiGianGui;

    private Long taiKhoanId;
    private String tenNguoiNhan;
    private Long baoCaoId;
}