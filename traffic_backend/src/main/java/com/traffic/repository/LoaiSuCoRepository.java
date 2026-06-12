package com.traffic.repository;

import com.traffic.entity.LoaiSuCo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoaiSuCoRepository extends JpaRepository<LoaiSuCo, Integer> {
}