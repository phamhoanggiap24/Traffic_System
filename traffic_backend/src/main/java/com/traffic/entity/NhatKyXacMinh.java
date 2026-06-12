package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "nhat_ky_xac_minh")
@Data
public class NhatKyXacMinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long nhatKyId;

    @ManyToOne
    @JoinColumn(name = "bao_cao_id")
    private BaoCaoSuCo baoCao;

    @ManyToOne
    @JoinColumn(name = "tai_khoan_id")
    private TaiKhoan taiKhoan;

    private String hanhDong;
    private String lyDo;

    @CreationTimestamp
    private LocalDateTime thoiGian;
}