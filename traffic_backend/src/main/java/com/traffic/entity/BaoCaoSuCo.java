package com.traffic.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.traffic.common.ReportStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "bao_cao_su_co")
@Data
public class BaoCaoSuCo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long baoCaoId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    @ManyToOne
    @JoinColumn(name = "loai_su_co_id")
    private LoaiSuCo loaiSuCo;

    @Column(columnDefinition = "VARCHAR(200)")
    private String moTa;

    private Double viDo;
    private Double kinhDo;
    private String hinhAnhUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private ReportStatus trangThai;

    private Integer doTinCayBaoCao;

    private LocalDateTime thoiGianBaoCao;
    private LocalDateTime thoiGianXacMinh;
}