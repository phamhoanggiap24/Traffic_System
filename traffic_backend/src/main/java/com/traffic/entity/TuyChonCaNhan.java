package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tuy_chon_ca_nhan")
@Data
public class TuyChonCaNhan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tuyChonId;

    @OneToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    private Integer nguongUnTac;
    private Boolean nhanThongBao;
    private Float banKinhCanhBao;

    private Double viDoHienTai;
    private Double kinhDoHienTai;
    private LocalDateTime thoiGianCapNhatViTri;
}