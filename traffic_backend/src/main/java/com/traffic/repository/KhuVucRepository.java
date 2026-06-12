package com.traffic.repository;

import com.traffic.entity.KhuVuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KhuVucRepository extends JpaRepository<KhuVuc, Long> {
    @Query(value = "SELECT COUNT(*) FROM khu_vuc k WHERE " +
            "(6371000 * acos(cos(radians(:lat)) * cos(radians(k.vi_do)) * cos(radians(k.kinh_do) - radians(:lng)) + sin(radians(:lat)) * sin(radians(k.vi_do)))) <= :radius",
            nativeQuery = true)
    long countCamerasWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radius") double radius);
}