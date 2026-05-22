package com.example.Naengbuhae.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 모바일 앱에서 시작한 OAuth 흐름을 표시한다.
// 앱(flutter_web_auth_2)이 외부 브라우저(Custom Tab)로 /oauth2/authorization/{provider}?client=app
// 을 열면 oauth_client=app 쿠키를 심는다. provider 콜백까지 모두 같은 도메인이라 쿠키가 round-trip되고,
// OAuth2SuccessHandler/FailureHandler가 이 쿠키를 보고 웹(http) 대신 앱 커스텀 스킴으로 리다이렉트한다.
// (구글은 임베디드 WebView OAuth를 disallowed_useragent로 차단하므로 앱도 외부 브라우저로 로그인해야 함)
public class OAuthClientTypeFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "oauth_client";
    public static final String APP_VALUE = "app";

    // 앱이 OAuth 결과를 받을 커스텀 URL 스킴. 환경 무관하게 고정값.
    public static final String APP_REDIRECT = "naengbuhae://oauth/callback";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/oauth2/authorization/")
                && APP_VALUE.equals(request.getParameter("client"))) {
            Cookie cookie = new Cookie(COOKIE_NAME, APP_VALUE);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(600); // OAuth 1회 흐름엔 충분 (10분)
            cookie.setAttribute("SameSite", "Lax"); // provider→백엔드 top-level redirect에서 쿠키 유지
            response.addCookie(cookie);
        }
        filterChain.doFilter(request, response);
    }

    // 콜백 요청에 oauth_client=app 쿠키가 있으면 앱에서 온 OAuth 흐름.
    public static boolean isAppClient(HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        for (Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName()) && APP_VALUE.equals(c.getValue())) {
                return true;
            }
        }
        return false;
    }

    // 흐름 종료 후 쿠키 제거 — 같은 브라우저(Custom Tab)의 이후 웹 로그인에 영향 없게.
    public static void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
