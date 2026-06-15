package com.traffic.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.traffic.common.UserStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tai_khoan")
@Data
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

    // Quan hệ với bảng phân quyền (thực thể trung gian)
    // CascadeType.ALL: Khi xóa tài khoản, các bản ghi phân quyền liên quan sẽ bị xóa theo
    // orphanRemoval = true: Khi xóa một phân quyền khỏi danh sách này, nó sẽ bị xóa khỏi DB
    @OneToMany(mappedBy = "taiKhoan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PhanQuyen> danhSachPhanQuyen = new ArrayList<>();

    // Quan hệ với các bảng nghiệp vụ khác theo ERD của bạn
    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    private List<BaoCaoSuCo> danhSachBaoCao;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    private List<CanhBao> danhSachCanhBao;

    @OneToOne(mappedBy = "taiKhoan", cascade = CascadeType.ALL)
    @JsonIgnore
    private TuyChonCaNhan tuyChonCaNhan;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    private List<NhatKyHeThong> nhatKyHeThong;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    private List<TokenXacThuc> danhSachToken;

    @OneToMany(mappedBy = "taiKhoan")
    @JsonIgnore
    private List<NhatKyXacMinh> nhatKyXacMinh;

}