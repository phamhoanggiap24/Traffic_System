package com.traffic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "khu_vuc")
@Data
public class KhuVuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer khuVucId;

    private String tenKhuVuc;
    private Double viDo;
    private Double kinhDo;
    private Float banKinh;
}