package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 비밀번호 재설정용 일회성 토큰.
// PASSWORD_RESET: "비번 잊었어요" 요청 시 발송, 30분 유효.
// (과거 EMAIL_VERIFY는 회원가입 직후 매직 링크용이었으나 코드 방식으로 전환되며 사용 중단.
//  스키마에 남은 enum 값과 기존 row 호환을 위해 enum 자체는 보존.)
@Entity
@Table(name = "user_tokens",
        indexes = @Index(name = "idx_user_tokens_token", columnList = "token", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class UserToken {

    public enum Type { EMAIL_VERIFY, PASSWORD_RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserToken(User user, String token, Type type, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
