package com.example.Naengbuhae.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientIpUtilTest {

    @Test
    @DisplayName("X-Forwarded-For 우선 — 단일 IP")
    void xForwardedForSingleIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For 다중 IP — 콤마 구분, 첫 번째(원래 클라이언트) 반환")
    void xForwardedForMultipleIps() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.7, 192.0.2.1");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("203.0.113.1");
    }

    @Test
    @DisplayName("X-Forwarded-For 없으면 Proxy-Client-IP 사용")
    void fallbackToProxyClientIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("198.51.100.5");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("198.51.100.5");
    }

    @Test
    @DisplayName("모든 프록시 헤더 없으면 remoteAddr 사용")
    void fallbackToRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("값이 'unknown'(case-insensitive)이면 무시하고 다음 헤더 시도")
    void unknownIsIgnored() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("UNKNOWN");
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn("198.51.100.10");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("198.51.100.10");
    }

    @Test
    @DisplayName("빈 문자열이면 무시하고 다음 헤더 시도")
    void emptyHeaderIsIgnored() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("198.51.100.20");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("198.51.100.20");
    }

    @Test
    @DisplayName("HTTP_CLIENT_IP / HTTP_X_FORWARDED_FOR도 인식 (CGI 스타일 헤더)")
    void cgiStyleHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("198.51.100.30");

        assertThat(ClientIpUtil.getClientIp(request)).isEqualTo("198.51.100.30");
    }
}
