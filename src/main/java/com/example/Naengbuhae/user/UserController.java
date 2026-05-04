package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import com.example.Naengbuhae.config.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

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
            return new LoginResponse(false, "로그인 실패", null);
        }

        String token = jwtUtil.createToken(user.getUsername(), user.getRole());
        return new LoginResponse(true, "로그인 성공", token);
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
}
