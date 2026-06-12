package com.traffic.repository;

import com.traffic.entity.NhatKyXacMinh;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NhatKyXacMinhRepository extends JpaRepository<NhatKyXacMinh, Long> {
    List<NhatKyXacMinh> findByBaoCaoBaoCaoId(Long baoCaoId);
}