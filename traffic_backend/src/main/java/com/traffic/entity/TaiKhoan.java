package com.traffic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.traffic.common.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tai_khoan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taiKhoanId;

    @Column(unique = true, nullable = false)
    private String tenDangNhap;

    @Column(nullable = false)
    @JsonIgnore
    private String matKhau;

    @Column(unique = true)
    private String email;

    private String hoTen;

    private String soDienThoai;

    private String verificationToken;

    private LocalDateTime tokenCreatedAt;

    @Enumerated(EnumType.STRING)
    private UserStatus trangThai;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime ngayTao;

    @Column(nullable = false)
    @Builder.Default
    private Integer doTinCayNguoiDung = 50;

    @UpdateTimestamp
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "taiKhoan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<PhanQuyen> danhSachPhanQuyen = new ArrayList<>();

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<BaoCaoSuCo> danhSachBaoCao;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CanhBao> danhSachCanhBao;

    @OneToOne(mappedBy = "taiKhoan", cascade = CascadeType.ALL)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TuyChonCaNhan tuyChonCaNhan;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<NhatKyHeThong> nhatKyHeThong;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TokenXacThuc> danhSachToken;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<NhatKyXacMinh> nhatKyXacMinh;
}