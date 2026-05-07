package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// access token 재발급용 refresh token. 단순 UUID 문자열로 발급되어 DB에서 검증.
// 로그아웃/탈퇴 시 row를 삭제해서 즉시 무효화 가능 (JWT처럼 서명만으로 못 끊는 한계 회피).
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

    public RefreshToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
