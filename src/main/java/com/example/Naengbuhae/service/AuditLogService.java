package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.AuditLog;
import com.example.Naengbuhae.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String adminName, String action, String targetType, Long targetId, String description, String ip) {
        AuditLog log = new AuditLog(adminName, action, targetType, targetId, description, ip);
        auditLogRepository.save(log);
    }
}