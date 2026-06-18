package com.traffic.repository;

import com.traffic.entity.TuyChonCaNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TuyChonCaNhanRepository extends JpaRepository<TuyChonCaNhan, Integer> {
    Optional<TuyChonCaNhan> findByTaiKhoanTaiKhoanId(Long taiKhoanId);

    @Query(value = """
SELECT *
FROM tuy_chon_ca_nhan t
WHERE t.nhan_thong_bao = true
  AND t.vi_do_hien_tai IS NOT NULL
  AND t.kinh_do_hien_tai IS NOT NULL
  AND t.thoi_gian_cap_nhat_vi_tri >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)
  AND (
    6371000 * ACOS(
      COS(RADIANS(:lat)) *
      COS(RADIANS(t.vi_do_hien_tai)) *
      COS(RADIANS(t.kinh_do_hien_tai) - RADIANS(:lng)) +
      SIN(RADIANS(:lat)) *
      SIN(RADIANS(t.vi_do_hien_tai))
    )
  ) <= COALESCE(t.ban_kinh_canh_bao, 100)
""", nativeQuery = true)
    List<TuyChonCaNhan> findUsersNearIncident(@Param("lat") Double lat, @Param("lng") Double lng);
}