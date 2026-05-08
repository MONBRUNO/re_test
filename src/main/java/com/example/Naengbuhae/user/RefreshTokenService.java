package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

// 발급/재발급/폐기 — refresh token 라이프사이클을 한곳에서 관리.
// rotation 정책: 재발급할 때마다 기존 refresh token은 폐기하고 새 token 발급.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.refresh-token-expiration-ms:31536000000}") // 365일 (rotation으로 활성 사용자는 사실상 영구)
    private long refreshTokenExpirationMs;

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

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("만료된 refresh token입니다. 다시 로그인해주세요.");
        }

        User user = stored.getUser();
        refreshTokenRepository.delete(stored);

        String newAccessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        String newRefreshToken = issue(user);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public record TokenPair(String accessToken, String refreshToken) {}
}
