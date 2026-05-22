package com.example.Naengbuhae.config;

import com.example.Naengbuhae.user.RefreshTokenService;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// OAuth 인증 성공 시: JWT 발급 → 프론트 콜백 페이지로 redirect
// 콜백 URL: {프론트}/oauth/callback?token=xxx&needsAdditionalInfo=true|false
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.frontend-redirect:http://localhost:5173/oauth/callback}")
    private String frontendRedirectUrl;

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        OAuth2UserInfo info = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByEmail(info.getEmail())
                .orElseThrow(() -> new IllegalStateException(
                        "OAuth 인증 후 DB에서 사용자를 찾을 수 없음: " + info.getEmail()));

        // JWT 발급 (access + refresh)
        String accessToken = jwtUtil.createToken(user.getUsername(), user.getRole());
        String refreshToken = refreshTokenService.issue(user);

        // 신체정보 등 필수값이 비어있으면 추가 정보 입력 필요
        boolean needsAdditionalInfo = !user.hasCompleteProfile();

        // 앱(외부 브라우저)에서 온 흐름이면 커스텀 스킴으로, 웹이면 프론트 URL로 리다이렉트
        boolean isApp = OAuthClientTypeFilter.isAppClient(request);
        String baseRedirect = isApp ? OAuthClientTypeFilter.APP_REDIRECT : frontendRedirectUrl;

        String redirectUrl = UriComponentsBuilder.fromUriString(baseRedirect)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("needsAdditionalInfo", needsAdditionalInfo)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();

        if (isApp) OAuthClientTypeFilter.clearCookie(response);

        log.info("[OAuth] 로그인 성공: {} ({}), 추가정보 필요: {}, 앱: {}",
                user.getEmail(), user.getProvider(), needsAdditionalInfo, isApp);

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
