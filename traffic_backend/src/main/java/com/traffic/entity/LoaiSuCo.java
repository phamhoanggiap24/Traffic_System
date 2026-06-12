package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "loai_su_co")
@Data
public class LoaiSuCo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer loaiSuCoId;

    private String tenLoai;

    @Column(columnDefinition = "VARCHAR(200)")
    private String moTa;
}