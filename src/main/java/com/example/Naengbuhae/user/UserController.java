package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import com.example.Naengbuhae.config.JwtUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ApiResponse signup(@RequestBody SignupRequest request) {

        String result = userService.signup(request.getUsername(), request.getPassword());

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

        String token = jwtUtil.createToken(user.getUsername());
        return new LoginResponse(true, "로그인 성공", token);
    }

    @GetMapping("/me")
    public ApiResponse me(@RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ApiResponse(false, "토큰이 없습니다.");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return new ApiResponse(false, "유효하지 않은 토큰입니다.");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        return new ApiResponse(true, "인증된 사용자: " + username);
    }
}