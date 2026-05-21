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

        // 프론트엔드 콜백 주소 뒤에 ?error=banned 를 붙여서 리다이렉트
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                .queryParam("error", errorMessage)
                .build()
                .toUriString();

        log.info("[OAuth Filter] 소셜 로그인 실패 처리 -> 프론트엔드 이관: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
