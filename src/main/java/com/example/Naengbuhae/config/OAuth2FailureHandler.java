package com.example.Naengbuhae.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.frontend-redirect:http://localhost:5173/oauth/callback}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String errorMessage = "auth_failed";

        // 우리가 CustomOAuth2UserService에서 던진 "banned_user" 예외인지 확인
        if (exception.getMessage() != null && exception.getMessage().contains("banned_user")) {
            errorMessage = "banned";
        }

        // 진짜 실패 원인 로깅 — provider 이메일 누락 / DB 중복 / FK 위반 등 추적용
        log.error("[OAuth Filter] 소셜 로그인 예외 원인", exception);
        Throwable cause = exception.getCause();
        if (cause != null) {
            log.error("[OAuth Filter] caused by", cause);
        }

        // 앱(외부 브라우저)에서 온 흐름이면 커스텀 스킴으로, 웹이면 프론트 URL로 리다이렉트
        boolean isApp = OAuthClientTypeFilter.isAppClient(request);
        String baseRedirect = isApp ? OAuthClientTypeFilter.APP_REDIRECT : frontendRedirectUrl;

        String targetUrl = UriComponentsBuilder.fromUriString(baseRedirect)
                .queryParam("error", errorMessage)
                .build()
                .toUriString();

        if (isApp) OAuthClientTypeFilter.clearCookie(response);

        log.info("[OAuth Filter] 소셜 로그인 실패 처리 -> 이관: {} (앱: {})", targetUrl, isApp);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
