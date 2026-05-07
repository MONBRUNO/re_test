package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTimestampDesc(); // 최신 로그부터 보기
}