package com.traffic.entity;

import com.traffic.common.ReadStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "canh_bao")
@Data
public class CanhBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long canhBaoId;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "bao_cao_id")
    private BaoCaoSuCo baoCaoSuCo;

    @ManyToOne
    @JoinColumn(name = "du_lieu_gt_id")
    private DuLieuGiaoThong duLieuGiaoThong;

    private String noiDung;
    private String loaiCanhBao;
    private String kenhGui;
    @Enumerated(EnumType.STRING)

    @Column(name = "trang_thai")
    private ReadStatus trangThai;

    private LocalDateTime thoiGianGui;
}