package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "quen_mat_khau")
@Data
public class QuenMatKhau {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quenMatKhauId;

    private String email;
    private String otpCode;
    private LocalDateTime thoiGianHetHan;
    private boolean daSuDung = false;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;
}