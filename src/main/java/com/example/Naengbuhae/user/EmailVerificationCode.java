package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 회원가입 화면에서 입력한 이메일에 6자리 코드를 보내고 확인받기 위한 임시 저장소.
// 가입 전이라 User row가 없으므로 UserToken과는 별도 테이블을 쓴다.
// 검증 완료 후 signup이 같은 이메일로 들어오면 row를 consume(삭제)하면서 user.emailVerified=true로 저장.
@Entity
@Table(name = "email_verification_codes",
       indexes = @Index(name = "idx_evc_email", columnList = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Setter
    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public EmailVerificationCode(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
