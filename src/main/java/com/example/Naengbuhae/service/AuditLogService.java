package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.AuditLog;
import com.example.Naengbuhae.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ✨ 1. @Async("auditTaskExecutor"): 전용 스레드 풀을 사용하여 서버 자원 보호 및 메인 로직 속도 저하 방지
    // ✨ 2. REQUIRES_NEW: 메인 로직이 롤백되어도 감사 로그는 무조건 DB에 남김!
    @Async("auditTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String adminName, String action, String targetType, Long targetId, String description, String ip) {
        try {
            AuditLog logEntity = new AuditLog(adminName, action, targetType, targetId, description, ip);
            auditLogRepository.save(logEntity);
            log.info("[Audit Log Saved] Admin: {}, Action: {}", adminName, action);
        } catch (Exception e) {
            log.error("[Audit Log Failed] DB 저장 실패: {}", e.getMessage());
        }
    }
}