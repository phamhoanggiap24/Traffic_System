package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_su_giao_thong")
@Data
public class LichSuGiaoThong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lichSuGtId;

    @ManyToOne
    @JoinColumn(name = "du_lieu_gt_id")
    private DuLieuGiaoThong duLieuGiaoThong;

    private Float tocDoTrungBinh;
    private Integer mucDoUnTac;
    private LocalDateTime thoiDiem;
}