package com.traffic.repository;

import com.traffic.entity.QuenMatKhau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuenMatKhauRepository extends JpaRepository<QuenMatKhau, Long> {
    Optional<QuenMatKhau> findFirstByEmailAndDaSuDungFalseOrderByThoiGianHetHanDesc(String email);
}