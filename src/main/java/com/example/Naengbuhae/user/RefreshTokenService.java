package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

// 발급/재발급/폐기 — refresh token 라이프사이클을 한곳에서 관리.
// rotation 정책: 재발급할 때마다 기존 refresh token은 revokedAt으로 폐기 처리하고 새 token 발급.
//
// 재사용 탐지(reuse detection):
//  - 폐기된 토큰이 다시 들어오면 도난 가능성 → 해당 사용자의 모든 토큰을 즉시 무효화.
//  - 단 grace period(기본 30초) 내 재제출은 multi-tab race(여러 탭 동시 401 → 동시 refresh)로
//    간주해 도난으로 처리하지 않음. invalid token으로만 응답해 자연스럽게 새 access를 받게 함.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.refresh-token-expiration-ms:31536000000}") // 365일 (rotation으로 활성 사용자는 사실상 영구)
    private long refreshTokenExpirationMs;

    @Value("${app.refresh-token.reuse-grace-seconds:30}")
    private long reuseGraceSeconds;

    @Transactional
    public String issue(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs));
        refreshTokenRepository.save(new RefreshToken(token, user, expiresAt));
        return token;
    }

    // refresh token 검증 → 새 access token 발급 + 기존 refresh 폐기 후 새 refresh 발급(rotation)
    @Transactional
    public TokenPair refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 refresh token입니다."));

        // 1) 이미 폐기된 토큰이 들어옴 — multi-tab race vs 도난 판별
        if (stored.isRevoked()) {
            LocalDateTime graceCutoff = LocalDateTime.now().minus(Duration.ofSeconds(reuseGraceSeconds));
            if (stored.getRevokedAt().isAfter(graceCutoff)) {
                // grace 내: 정상적인 multi-tab race로 추정. 그냥 invalid 처리.
                log.debug("[RefreshToken] grace 내 재제출 (multi-tab race 추정) userId={}",
                        stored.getUser().getId());
                throw new IllegalArgumentException("유효하지 않은 refresh token입니다.");
            }
            // grace 초과 후 재제출 = 도난 시나리오. 해당 사용자의 모든 토큰 즉시 무효화.
            log.warn("[RefreshToken] 재사용 탐지 — 사용자 모든 refresh token 무효화 userId={} revokedAt={}",
                    stored.getUser().getId(), stored.getRevokedAt());
            refreshTokenRepository.deleteByUser(stored.getUser());
            throw new IllegalArgumentException("세션이 무효화되었습니다. 보안 상의 이유로 다시 로그인해주세요.");
        }

        // 2) 자연 만료
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("만료된 refresh token입니다. 다시 로그인해주세요.");
        }

        // 3) 정상 rotation: 기존을 폐기 처리(delete가 아니라 revokedAt 마킹)하고 새 토큰 발급
        User user = stored.getUser();
        stored.revoke();

        String newAccessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        String newRefreshToken = issue(user);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // 로그아웃: 단일 token 폐기. delete 대신 revoke로 마킹해 후속 재제출도 탐지.
    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(stored -> {
            if (!stored.isRevoked()) stored.revoke();
        });
    }

    // 사용자 탈퇴/세션 강제 종료: 모든 토큰 일괄 삭제 (재사용 탐지 대상도 아님)
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
