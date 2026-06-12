package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "du_lieu_giao_thong")
@Data
public class DuLieuGiaoThong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long duLieuGtId;

    @ManyToOne
    @JoinColumn(name = "khu_vuc_id")
    private KhuVuc khuVuc;

    private Float tocDoTrungBinh;
    private Integer mucDoUnTac;
    private Float doTinCayDuLieu;
    private LocalDateTime thoiDiemGhiNhan;
    private String nguonDuLieu;
}