package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc(); // 최신 로그부터 보기

    // ✨ 지정된 날짜 이전의 로그를 싹 지우고, 지운 개수를 반환
    @Transactional
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    int deleteByTimestampBefore(LocalDateTime cutoffDate);
}
