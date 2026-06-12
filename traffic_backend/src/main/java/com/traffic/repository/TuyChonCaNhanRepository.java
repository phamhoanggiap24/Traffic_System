package com.traffic.repository;

import com.traffic.entity.TuyChonCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TuyChonCaNhanRepository extends JpaRepository<TuyChonCaNhan, Integer> {
    Optional<TuyChonCaNhan> findByTaiKhoanTaiKhoanId(Long taiKhoanId);
}