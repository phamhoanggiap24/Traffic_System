package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "nhat_ky_he_thong")
@Data
public class NhatKyHeThong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nhatKyHtId;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    private String hanhDong;

    @Column(columnDefinition = "VARCHAR(200)")
    private String moTa;
    private String diaChiIp;

    @CreationTimestamp
    private LocalDateTime thoiGian;
}