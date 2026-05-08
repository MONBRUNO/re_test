package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

// 매일 새벽 3시(서버 시간 기준)에 만료된/오래 폐기된 refresh token row를 일괄 삭제.
//   1) 자연 만료 (expires_at < now): rotation으로 폐기되지 않은 채 사용자 미접속으로 만료됨
//   2) 폐기 후 보존 기간 초과 (revoked_at < cutoff): 재사용 탐지 윈도우 종료된 row
// 재사용 탐지를 위해 rotation 시 즉시 삭제하지 않고 revokedAt 마킹만 하므로,
// 보존 기간(기본 24시간)이 지나면 정리해 테이블 비대화를 방지.
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.refresh-token.revoked-retention-hours:24}")
    private long revokedRetentionHours;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime revokedCutoff = now.minus(Duration.ofHours(revokedRetentionHours));
        int deleted = refreshTokenRepository.deleteExpiredOrOldRevoked(now, revokedCutoff);
        if (deleted > 0) {
            log.info("[RefreshTokenCleanup] 만료/오래 폐기된 refresh token {}건 삭제", deleted);
        }
    }
}
