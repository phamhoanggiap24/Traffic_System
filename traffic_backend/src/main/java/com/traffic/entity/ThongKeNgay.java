package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "thong_ke_ngay")
@Data
public class ThongKeNgay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long thongKeNgayId;

    @ManyToOne
    @JoinColumn(name = "khu_vuc_id")
    private KhuVuc khuVuc;

    private Float tocDoTrungBinh;
    private Float mucDoUnTacTrungBinh;
    private Float mucDoUnTacMax;

    @Temporal(TemporalType.DATE)
    private Date ngayThongKe;
}