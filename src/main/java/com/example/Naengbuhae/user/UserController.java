package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.domain.FcmToken;
import com.example.Naengbuhae.service.FcmTokenService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final EmailAuthService emailAuthService;
    private final FcmTokenService fcmTokenService;

    @PostMapping("/signup")
    // @Valid 어노테이션 추가로 SignupRequest DTO의 제약조건 발동
    public ApiResponse signup(@Valid @RequestBody SignupRequest request) {
        userService.signup(request);
        return new ApiResponse(true, "회원가입 성공");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        User user = userService.login(request.getUsername(), request.getPassword());

        if (user == null) {
            return new LoginResponse(false, "로그인 실패", null, null);
        }

        // 이메일+비번 가입자는 인증 완료 후에만 로그인 허용. OAuth 가입자는 emailVerified=true로 들어가서 통과.
        if (!user.isEmailVerified()) {
            return new LoginResponse(false,
                    "이메일 인증 후 로그인할 수 있어요. 가입 시 보낸 메일을 확인해주세요.",
                    null, null, true, user.getEmail());
        }

        String accessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(true, "로그인 성공", accessToken, refreshToken);
    }

    // === 회원가입 화면 인라인 인증번호 ===

    // 이메일에 6자리 인증번호 발송. 가입 전이라 미로그인 상태.
    // body: {"email": "..."}
    @PostMapping("/email/send-code")
    public ApiResponse sendSignupCode(@RequestBody Map<String, String> body) {
        emailAuthService.sendSignupVerificationCode(body.get("email"));
        return new ApiResponse(true, "인증번호를 보냈어요.");
    }

    // 입력한 인증번호 검증. 가입 폼 제출 전 별도로 호출.
    // body: {"email": "...", "code": "123456"}
    @PostMapping("/email/verify-code")
    public ApiResponse verifySignupCode(@RequestBody Map<String, String> body) {
        emailAuthService.verifySignupCode(body.get("email"), body.get("code"));
        return new ApiResponse(true, "이메일 인증이 완료되었어요.");
    }

    // 미인증 사용자가 로그인 화면에서 "메일 다시 받기"를 눌렀을 때 호출.
    // username/password를 다시 받아 검증 → 비번 맞고 미인증 상태일 때만 재발송. 이메일 enumeration 방지.
    @PostMapping("/resend-verification-public")
    public ApiResponse resendVerificationPublic(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        if (user == null) {
            return new ApiResponse(false, "아이디 또는 비밀번호를 확인해주세요.");
        }
        if (user.isEmailVerified()) {
            return new ApiResponse(false, "이미 이메일 인증이 완료된 계정이에요.");
        }
        emailAuthService.resendVerificationEmail(user.getUsername());
        return new ApiResponse(true, "인증 메일을 다시 보냈어요.");
    }

    // access token 만료 시 refresh token으로 새 access(+새 refresh) 발급
    // body: {"refreshToken": "..."}
    @PostMapping("/token/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refreshToken이 필요합니다.");
        }
        RefreshTokenService.TokenPair pair = refreshTokenService.refresh(refreshToken);
        return Map.of(
                "token", pair.accessToken(),
                "refreshToken", pair.refreshToken()
        );
    }

    // 로그아웃: 서버에서 refresh token 무효화 (access는 stateless라 클라가 버리면 됨)
    // body: {"refreshToken": "..."}
    @PostMapping("/logout")
    public ApiResponse logout(@RequestBody(required = false) Map<String, String> body) {
        if (body != null) {
            String refreshToken = body.get("refreshToken");
            if (refreshToken != null && !refreshToken.isBlank()) {
                refreshTokenService.revoke(refreshToken);
            }
        }
        return new ApiResponse(true, "로그아웃 되었습니다.");
    }

    // 현재 로그인한 사용자의 전체 프로필 조회
    // SecurityFilter가 이미 토큰 검증 + Principal 주입을 처리하므로 직접 파싱 불필요
    @GetMapping("/me")
    public UserResponseDto me(Principal principal) {
        return userService.getMyProfile(principal.getName());
    }

    // 프로필 수정 (신체정보 변경 시 권장 칼로리 자동 재계산)
    @PutMapping("/me")
    public UserResponseDto updateMe(@Valid @RequestBody ProfileUpdateRequest request,
                                    Principal principal) {
        return userService.updateMyProfile(principal.getName(), request);
    }

    // 회원 탈퇴: 본인의 모든 식재료/레시피/장보기까지 함께 삭제
    @DeleteMapping("/me")
    public ApiResponse deleteMe(Principal principal) {
        userService.deleteMyAccount(principal.getName());
        return new ApiResponse(true, "회원 탈퇴가 완료되었습니다.");
    }

    // === 이메일 인증 ===

    // 메일에 든 링크에서 호출. token만으로 인증 (인증 안 된 사용자도 호출하므로 무인증 허용).
    @PostMapping("/verify-email")
    public ApiResponse verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("토큰이 필요합니다.");
        }
        String name = emailAuthService.verifyEmail(token);
        return new ApiResponse(true, name + "님, 이메일 인증이 완료되었습니다.");
    }

    // 마이페이지에서 인증 메일 재발송 (로그인 필요).
    @PostMapping("/resend-verification")
    public ApiResponse resendVerification(Principal principal) {
        emailAuthService.resendVerificationEmail(principal.getName());
        return new ApiResponse(true, "인증 메일을 재발송했습니다.");
    }

    // === 비밀번호 찾기/재설정 ===

    // 비번 찾기: 이메일 입력 → 재설정 메일 발송. 존재 여부와 무관하게 같은 응답(보안).
    @PostMapping("/password/forgot")
    public ApiResponse forgotPassword(@RequestBody Map<String, String> body) {
        emailAuthService.requestPasswordReset(body.get("email"));
        return new ApiResponse(true, "재설정 안내 메일을 보냈습니다. 메일함을 확인해주세요.");
    }

    // 재설정 페이지에서 토큰 + 새 비번.
    @PostMapping("/password/reset")
    public ApiResponse resetPassword(@RequestBody Map<String, String> body) {
        emailAuthService.resetPassword(body.get("token"), body.get("newPassword"));
        return new ApiResponse(true, "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.");
    }

    // === FCM 푸시 알림 토큰 ===

    // 앱에서 발급받은 디바이스 토큰을 서버에 등록.
    // body: {"token": "...", "platform": "ANDROID|IOS|WEB"}
    @PostMapping("/fcm-tokens")
    public ApiResponse registerFcmToken(@RequestBody Map<String, String> body, Principal principal) {
        String token = body.get("token");
        String platformStr = body.getOrDefault("platform", "ANDROID");
        FcmToken.Platform platform;
        try {
            platform = FcmToken.Platform.valueOf(platformStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            platform = FcmToken.Platform.ANDROID;
        }
        fcmTokenService.register(principal.getName(), token, platform);
        return new ApiResponse(true, "FCM 토큰이 등록되었습니다.");
    }

    // 로그아웃/탈퇴 직전 호출.
    @DeleteMapping("/fcm-tokens/{token}")
    public ApiResponse unregisterFcmToken(@PathVariable String token) {
        fcmTokenService.unregister(token);
        return new ApiResponse(true, "FCM 토큰이 해제되었습니다.");
    }
}
