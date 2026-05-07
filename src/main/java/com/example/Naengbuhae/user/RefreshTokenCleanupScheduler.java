package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 매일 새벽 3시(서버 시간 기준)에 만료된 refresh token row를 일괄 삭제.
// rotation 정책상 재발급할 때마다 옛 토큰은 즉시 삭제되지만,
// 사용자가 14일간 재방문하지 않으면 만료된 row가 그대로 남아 쌓이므로 주기 청소가 필요.
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("[RefreshTokenCleanup] 만료된 refresh token {}건 삭제", deleted);
        }
    }
}
