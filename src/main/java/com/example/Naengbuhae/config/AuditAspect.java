package com.example.Naengbuhae.config;

import com.example.Naengbuhae.service.AuditLogService;
import com.example.Naengbuhae.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    // @Audit 어노테이션이 붙은 메서드가 "성공적으로 끝났을 때만(@AfterReturning)" 실행
    @AfterReturning(value = "@annotation(auditAnnotation)")
    public void logAuditActivity(JoinPoint joinPoint, Audit auditAnnotation) {

        // 1. 관리자 이름 가져오기
        String adminName = "UNKNOWN";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            adminName = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        // 2. 실제 클라이언트 IP 추출
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String clientIp = ClientIpUtil.getClientIp(request);

        // 3. 대상 ID 가져오기 (컨트롤러의 첫 번째 파라미터를 타겟 ID로 간주)
        Long targetId = null;
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof Long) {
            targetId = (Long) args[0];
        }

        // 4. 비동기 로그 저장 호출
        auditLogService.logAction(
                adminName,
                auditAnnotation.action(),
                auditAnnotation.targetType(),
                targetId,
                auditAnnotation.description().isEmpty() ? "관리자 작업" : auditAnnotation.description(),
                clientIp
        );
    }
}