package com.traffic.repository;

import com.traffic.entity.PhanQuyen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PhanQuyenRepository extends JpaRepository<PhanQuyen, Long> {
    List<PhanQuyen> findByTaiKhoanTaiKhoanId(Long taiKhoanId);
}