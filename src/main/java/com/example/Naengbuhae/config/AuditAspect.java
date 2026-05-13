package com.example.Naengbuhae.config;

import com.example.Naengbuhae.service.AuditLogService;
import com.example.Naengbuhae.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
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

        // 3. 대상 ID 가져오기
        Long targetId = null;
        String idParamName = auditAnnotation.idParamName();
        Object[] args = joinPoint.getArgs();

        if (!idParamName.isEmpty()) {
            // ✨ 1. idParamName이 지정된 경우: 파라미터 이름으로 찾기
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(idParamName) && args[i] instanceof Long) {
                    targetId = (Long) args[i];
                    break;
                }
            }
        } else if (args.length > 0 && args[0] instanceof Long) {
            // 2. 지정 안 된 경우: 기존처럼 첫 번째 Long 인자 가져오기 (하위 호환)
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
