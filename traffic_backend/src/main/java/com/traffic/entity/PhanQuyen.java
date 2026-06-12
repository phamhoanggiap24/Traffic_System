package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "phan_quyen",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"tai_khoan_id", "vai_tro_id"})
        }
)
@Data
public class PhanQuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long phanQuyenId;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "vai_tro_id")
    private VaiTro vaiTro;
}