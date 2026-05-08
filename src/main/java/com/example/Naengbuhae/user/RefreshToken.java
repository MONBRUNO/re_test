package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// access token 재발급용 refresh token. 단순 UUID 문자열로 발급되어 DB에서 검증.
// 로그아웃/탈퇴 시 row를 삭제해서 즉시 무효화 가능 (JWT처럼 서명만으로 못 끊는 한계 회피).
//
// 재사용 탐지: rotation 시 delete 대신 revokedAt 마킹. 폐기된 토큰이 다시 들어오면
// 도난 시나리오로 간주해 해당 사용자의 모든 refresh token을 무효화한다.
// (단 짧은 grace period 내 재제출은 multi-tab race로 보고 무시 — 서비스 계층에서 처리)
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_user", columnList = "user_id"))
@Getter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // null = 활성, non-null = 폐기됨 (rotation 또는 로그아웃 시점 기록)
    private LocalDateTime revokedAt;

    public RefreshToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
}
