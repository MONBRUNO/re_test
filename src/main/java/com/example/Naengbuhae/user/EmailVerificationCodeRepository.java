package com.example.Naengbuhae.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    // 같은 이메일의 가장 최근 코드. 검증 시 이것만 본다.
    Optional<EmailVerificationCode> findTopByEmailOrderByCreatedAtDesc(String email);

    // 가입 시 검증된 코드가 있는지 확인 — 만료 안 된 verified=true 1건이라도 있으면 true.
    @Query("SELECT COUNT(c) > 0 FROM EmailVerificationCode c " +
           "WHERE c.email = :email AND c.verified = true AND c.expiresAt > :now")
    boolean hasValidVerifiedCode(@Param("email") String email, @Param("now") LocalDateTime now);

    // 가입 완료 후 정리 + 같은 이메일로 재발송 시 기존 코드 무효화.
    @Modifying
    void deleteByEmail(String email);
}
