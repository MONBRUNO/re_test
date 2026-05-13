package com.example.Naengbuhae.config;

import com.example.Naengbuhae.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogCleanupScheduler {

    private final AuditLogRepository auditLogRepository;

    @Value("${app.audit.retention-days:180}") // 기본 180일 보관
    private int retentionDays;

    @Scheduled(cron = "0 0 4 1 * *") // 매월 1일 새벽 4시에 실행
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        
        // ✨ 이제 Repository에 메서드가 있으므로 정상 작동합니다!
        int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffDate);
        
        if (deletedCount > 0) {
            log.info("[Audit Cleanup] {}일 이전의 낡은 감사 로그 {}건을 성공적으로 정리했습니다.", retentionDays, deletedCount);
        } else {
            log.debug("[Audit Cleanup] 정리할 오래된 로그가 없습니다.");
        }
    }
}
