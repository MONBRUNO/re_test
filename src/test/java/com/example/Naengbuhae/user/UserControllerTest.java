package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.CustomOAuth2UserService;
import com.example.Naengbuhae.config.JwtAuthenticationFilter;
import com.example.Naengbuhae.config.JwtUtil;
import com.example.Naengbuhae.config.OAuth2SuccessHandler;
import com.example.Naengbuhae.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean UserService userService;
    @MockBean RefreshTokenService refreshTokenService;

    // SecurityConfig 빈 그래프용
    @MockBean JwtUtil jwtUtil;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean CustomOAuth2UserService customOAuth2UserService;
    @MockBean OAuth2SuccessHandler oAuth2SuccessHandler;

    Principal alice = () -> "alice";

    @Test
    @DisplayName("POST /user/signup 성공 → ApiResponse(success: true, message: 회원가입 성공)")
    void signupSuccess() throws Exception {
        when(userService.signup(any())).thenReturn("회원가입 성공");

        mockMvc.perform(post("/user/signup")
                        .content(mapper.writeValueAsString(validSignup()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("POST /user/signup 실패 (중복 등) → ApiResponse(success: false, 서비스 메시지)")
    void signupBusinessFailure() throws Exception {
        when(userService.signup(any())).thenReturn("이미 존재하는 아이디입니다.");

        mockMvc.perform(post("/user/signup")
                        .content(mapper.writeValueAsString(validSignup()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 비즈니스 실패는 200 + success:false (기존 컨벤션)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("이미 존재")));
    }

    @Test
    @DisplayName("POST /user/login 성공 → access + refresh token 반환")
    void loginSuccess() throws Exception {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getUsername()).thenReturn("alice");
        when(user.getRole()).thenReturn(UserRole.USER);
        when(userService.login("alice", "ValidPwd1!")).thenReturn(user);
        when(jwtUtil.createToken("alice", UserRole.USER)).thenReturn("access-jwt");
        when(refreshTokenService.issue(user)).thenReturn("refresh-uuid");

        mockMvc.perform(post("/user/login")
                        .content("{\"username\":\"alice\",\"password\":\"ValidPwd1!\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("access-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"));
    }

    @Test
    @DisplayName("POST /user/login 실패 (잘못된 비번) → success:false, 토큰 null")
    void loginFailure() throws Exception {
        when(userService.login("alice", "wrong")).thenReturn(null);

        mockMvc.perform(post("/user/login")
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("POST /user/token/refresh → 새 access/refresh token")
    void tokenRefresh() throws Exception {
        when(refreshTokenService.refresh("old-refresh"))
                .thenReturn(new RefreshTokenService.TokenPair("new-access", "new-refresh"));

        mockMvc.perform(post("/user/token/refresh")
                        .content("{\"refreshToken\":\"old-refresh\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @DisplayName("POST /user/token/refresh body에 refreshToken 누락 → 400")
    void tokenRefreshMissingBody() throws Exception {
        mockMvc.perform(post("/user/token/refresh")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("refreshToken")));
    }

    @Test
    @DisplayName("POST /user/logout → revoke 호출 후 success")
    void logout() throws Exception {
        mockMvc.perform(post("/user/logout")
                        .content("{\"refreshToken\":\"some-token\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(refreshTokenService).revoke("some-token");
    }

    @Test
    @DisplayName("POST /user/logout body 없음 → revoke 호출 안 됨, success는 그대로 (멱등)")
    void logoutNoBody() throws Exception {
        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    @DisplayName("DELETE /user/me → deleteMyAccount 호출 + 탈퇴 메시지")
    void deleteMe() throws Exception {
        doNothing().when(userService).deleteMyAccount("alice");

        mockMvc.perform(delete("/user/me").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("탈퇴")));

        verify(userService).deleteMyAccount("alice");
    }

    @Test
    @DisplayName("GET /user/me → getMyProfile 결과 그대로 반환")
    void getMe() throws Exception {
        UserResponseDto dto = org.mockito.Mockito.mock(UserResponseDto.class);
        when(dto.getUsername()).thenReturn("alice");
        when(dto.getName()).thenReturn("홍길동");
        when(userService.getMyProfile("alice")).thenReturn(dto);

        mockMvc.perform(get("/user/me").principal(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    private Map<String, Object> validSignup() {
        return Map.ofEntries(
                Map.entry("username", "alice123"),
                Map.entry("password", "ValidPwd1!"),
                Map.entry("name", "홍길동"),
                Map.entry("gender", "남"),
                Map.entry("birthDate", LocalDate.of(1995, 5, 15).toString()),
                Map.entry("height", 175.0),
                Map.entry("weight", 70.0),
                Map.entry("email", "alice@example.com"),
                Map.entry("activityLevel", "보통 활동"),
                Map.entry("dietGoal", "체중 유지")
        );
    }
}
