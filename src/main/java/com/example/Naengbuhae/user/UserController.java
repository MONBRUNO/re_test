package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import com.example.Naengbuhae.config.JwtUtil;
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

    @PostMapping("/signup")
    // @Valid 어노테이션 추가로 SignupRequest DTO의 제약조건 발동
    public ApiResponse signup(@Valid @RequestBody SignupRequest request) {

        String result = userService.signup(request);

        if (result.equals("회원가입 성공")) {
            return new ApiResponse(true, result);
        } else {
            return new ApiResponse(false, result);
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

        User user = userService.login(request.getUsername(), request.getPassword());

        if (user == null) {
            return new LoginResponse(false, "로그인 실패", null, null);
        }

        String accessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.issue(user);
        return new LoginResponse(true, "로그인 성공", accessToken, refreshToken);
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
}
