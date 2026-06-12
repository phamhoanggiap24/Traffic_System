package com.traffic.repository;

import com.traffic.entity.TokenXacThuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenXacThucRepository extends JpaRepository<TokenXacThuc, Long> {
    Optional<TokenXacThuc> findByTokenChuoi(String tokenChuoi);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenXacThuc t WHERE t.thoiGianHetHan < ?1")
    void deleteExpiredTokens(LocalDateTime now);
}