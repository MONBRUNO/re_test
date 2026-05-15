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
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32; // base64로 약 43자
    private static final int RESET_TTL_MINUTES = 30;
    // 회원가입 인라인 인증번호 TTL — 사용자가 받자마자 입력하는 패턴이라 짧게.
    private static final int SIGNUP_CODE_TTL_MINUTES = 10;
    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/])[A-Za-z0-9!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/]{8,}$";

    @Value("${app.mail.web-base-url:http://localhost:5173}")
    private String webBaseUrl;

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

    // === 회원가입 화면용: 6자리 인증번호 발송 ===
    // 기존에 같은 이메일로 남아있던 코드는 삭제 후 새로 발급 (재요청해도 가장 최신 1건만 유효).
    // 이미 회원가입된 이메일이면 즉시 거부 — 이메일 enumeration보다 명확한 UX 우선.
    @Transactional
    public void sendSignupVerificationCode(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalized).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        emailVerificationCodeRepository.deleteByEmail(normalized);

        String code = generate6DigitCode();
        emailVerificationCodeRepository.save(new EmailVerificationCode(
                normalized, code,
                LocalDateTime.now().plusMinutes(SIGNUP_CODE_TTL_MINUTES)
        ));

        mailService.sendHtml(
                normalized,
                "[냉부해] 회원가입 인증번호",
                MailTemplates.verifyEmailCode(code)
        );
    }

    // 사용자가 입력한 코드 검증 — 통과하면 EmailVerificationCode.verified=true.
    @Transactional
    public void verifySignupCode(String email, String code) {
        if (email == null || email.isBlank() || code == null || code.isBlank()) {
            throw new IllegalArgumentException("이메일과 인증번호를 입력해주세요.");
        }
        String normalized = email.trim().toLowerCase();
        EmailVerificationCode row = emailVerificationCodeRepository
                .findTopByEmailOrderByCreatedAtDesc(normalized)
                .orElseThrow(() -> new IllegalArgumentException("인증번호를 먼저 요청해주세요."));

        if (row.isExpired()) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 받아주세요.");
        }
        if (!row.getCode().equals(code.trim())) {
            throw new IllegalArgumentException("인증번호가 일치하지 않습니다.");
        }
        row.setVerified(true);
    }

    // 회원가입 시점에 호출 — 검증된 코드가 살아있으면 true, 동시에 모두 정리.
    @Transactional
    public boolean consumeVerifiedSignupCode(String email) {
        if (email == null || email.isBlank()) return false;
        String normalized = email.trim().toLowerCase();
        boolean ok = emailVerificationCodeRepository.hasValidVerifiedCode(normalized, LocalDateTime.now());
        if (ok) {
            emailVerificationCodeRepository.deleteByEmail(normalized);
        }
        return ok;
    }

    private String generate6DigitCode() {
        // 000000~999999 균등. 앞 0이 잘리지 않도록 String.format 사용.
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
