package com.example.Naengbuhae.user;

import com.example.Naengbuhae.service.MailService;
import com.example.Naengbuhae.service.MailTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

// 이메일 인증 + 비밀번호 재설정 흐름을 모두 다룬다.
// 토큰 생성/발송/검증 로직이 거의 동일하므로 한 서비스에 묶음.
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32; // base64로 약 43자
    private static final int VERIFY_TTL_HOURS = 24;
    private static final int RESET_TTL_MINUTES = 30;
    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/])[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/]{8,}$";

    @Value("${app.mail.web-base-url:http://localhost:5173}")
    private String webBaseUrl;

    // === 회원가입 직후 호출: 인증 메일 발송 ===
    @Transactional
    public void sendVerificationEmail(User user) {
        if (user.isEmailVerified()) return;
        userTokenRepository.invalidateOlder(user, UserToken.Type.EMAIL_VERIFY);

        String token = generateToken();
        userTokenRepository.save(new UserToken(
                user, token, UserToken.Type.EMAIL_VERIFY,
                LocalDateTime.now().plusHours(VERIFY_TTL_HOURS)
        ));

        String url = webBaseUrl + "/verify-email?token=" + token;
        try {
            mailService.sendHtml(
                    user.getEmail(),
                    "[냉부해] 이메일 인증을 완료해주세요",
                    MailTemplates.verifyEmail(user.getName(), url)
            );
        } catch (Exception e) {
            // 메일 발송 실패는 가입 자체를 막지 않음 — 마이페이지에서 재발송 가능하게.
            log.warn("[EmailAuth] 인증 메일 발송 실패 (가입은 진행됨): {}", e.getMessage());
        }
    }

    // 마이페이지에서 "인증 메일 다시 보내기" 클릭 시 호출.
    @Transactional
    public void resendVerificationEmail(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("이미 인증된 이메일입니다.");
        }
        sendVerificationEmail(user);
    }

    // 메일 링크 클릭 → 토큰 검증 + emailVerified=true.
    @Transactional
    public String verifyEmail(String token) {
        UserToken t = userTokenRepository.findByTokenAndType(token, UserToken.Type.EMAIL_VERIFY)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 인증 링크입니다."));
        if (t.isUsed()) throw new IllegalArgumentException("이미 사용된 인증 링크입니다.");
        if (t.isExpired()) throw new IllegalArgumentException("만료된 인증 링크입니다. 새로 요청해주세요.");

        User user = t.getUser();
        user.setEmailVerified(true);
        t.setUsed(true);
        log.info("[EmailAuth] 이메일 인증 완료: {}", user.getUsername());
        return user.getName();
    }

    // === 비번 찾기 요청: 메일 발송 ===
    // 보안상 "이메일이 존재하지 않습니다"는 알려주지 않음 (사용자 enumeration 방지).
    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        var userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            log.info("[EmailAuth] 비번 재설정 요청 — 존재하지 않는 이메일: {}", email);
            return; // 성공 응답 (보안)
        }
        User user = userOpt.get();
        if (user.getProvider() != OAuthProvider.LOCAL) {
            // OAuth 가입자는 비번이 없음 — 그래도 보안 위해 같은 응답
            log.info("[EmailAuth] 비번 재설정 요청 — OAuth 가입자: {}", email);
            return;
        }

        userTokenRepository.invalidateOlder(user, UserToken.Type.PASSWORD_RESET);

        String token = generateToken();
        userTokenRepository.save(new UserToken(
                user, token, UserToken.Type.PASSWORD_RESET,
                LocalDateTime.now().plusMinutes(RESET_TTL_MINUTES)
        ));

        String url = webBaseUrl + "/reset-password?token=" + token;
        mailService.sendHtml(
                user.getEmail(),
                "[냉부해] 비밀번호 재설정 안내",
                MailTemplates.passwordReset(user.getName(), url)
        );
    }

    // 재설정 페이지에서 토큰 + 새 비밀번호 → 비밀번호 변경.
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        if (newPassword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            throw new IllegalArgumentException("비밀번호에 한글은 사용할 수 없습니다.");
        }
        if (!newPassword.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영어 소문자, 숫자, 특수문자를 포함해야 합니다.");
        }

        UserToken t = userTokenRepository.findByTokenAndType(token, UserToken.Type.PASSWORD_RESET)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));
        if (t.isUsed()) throw new IllegalArgumentException("이미 사용된 링크입니다.");
        if (t.isExpired()) throw new IllegalArgumentException("만료된 링크입니다. 다시 요청해주세요.");

        User user = t.getUser();
        user.changePassword(passwordEncoder.encode(newPassword));
        t.setUsed(true);
        log.info("[EmailAuth] 비밀번호 재설정 완료: {}", user.getUsername());
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
