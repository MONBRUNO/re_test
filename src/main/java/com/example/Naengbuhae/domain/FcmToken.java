package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 모바일 앱(또는 웹) 디바이스의 FCM 등록 토큰.
// 한 유저가 여러 디바이스를 가질 수 있어 user_id + token 조합으로 unique.
// token 자체도 글로벌 유니크 (같은 디바이스가 두 유저에 묶이면 옛 유저 매핑은 제거).
@Entity
@Table(name = "fcm_tokens",
        indexes = {
                @Index(name = "idx_fcm_tokens_user", columnList = "user_id"),
                @Index(name = "idx_fcm_tokens_token", columnList = "token", unique = true)
        })
@Getter
@Setter
@NoArgsConstructor
public class FcmToken {

    public enum Platform { ANDROID, IOS, WEB }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastUsedAt = LocalDateTime.now();

    public FcmToken(User user, String token, Platform platform) {
        this.user = user;
        this.token = token;
        this.platform = platform;
    }
}
