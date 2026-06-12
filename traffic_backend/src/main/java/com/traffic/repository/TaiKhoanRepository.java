package com.traffic.repository;

import com.traffic.common.UserStatus;
import com.traffic.entity.TaiKhoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Long> {
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    Optional<TaiKhoan> findByEmail(String email);

    Boolean existsByTenDangNhap(String tenDangNhap);

    Boolean existsByEmail(String email);

    Optional<TaiKhoan> findByVerificationToken(String token);

    Page<TaiKhoan> findByTrangThaiNot(UserStatus status, Pageable pageable);

    Page<TaiKhoan> findByTenDangNhapContainingIgnoreCaseAndTrangThaiNot(String tenDangNhap, UserStatus status, Pageable pageable);
}