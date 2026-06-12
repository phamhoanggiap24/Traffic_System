package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_xac_thuc")
@Data
public class TokenXacThuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    private String tokenChuoi;
    private String loaiToken;
    private LocalDateTime thoiGianHetHan;
}