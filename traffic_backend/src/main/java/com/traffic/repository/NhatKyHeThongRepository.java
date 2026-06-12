package com.traffic.repository;

import com.traffic.entity.NhatKyHeThong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Long> {
    Page<NhatKyHeThong> findByTaiKhoanTaiKhoanId(Long taiKhoanId, Pageable pageable);
}