package com.example.Naengbuhae.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtil {
    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0]; // 다중 프록시일 경우 첫 번째(원래 클라이언트) IP 추출
            }
        }
        return request.getRemoteAddr();
    }
}