package com.example.Naengbuhae.config;

import com.example.Naengbuhae.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String AUTHORIZATION_KEY = "auth";

    // 💡 수정 1: 환경변수 누락 시 서버가 터지지 않도록 임시 기본값(최소 32자 이상) 빵빵하게 추가!
    @Value("${JWT_SECRET_KEY:default_secret_key_for_local_development_at_least_32_chars_long!}")
    private String SECRET_KEY;

    // access token 만료시간 (ms). 기본 30분. 테스트할 때만 짧게(예: 60000=1분) 두기.
    @Value("${app.jwt.access-token-expiration-ms:1800000}")
    private long EXPIRATION;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username, UserRole role) {
        return Jwts.builder()
                .subject(username)
                .claim(AUTHORIZATION_KEY, role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    // 💡 수정 2: 토큰 까보다가 에러 나도 서버 안 죽게 방어적 프로그래밍 적용!
    public UserRole getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        Object roleObj = claims.get(AUTHORIZATION_KEY);

        // 권한 정보가 없으면 기본 USER 권한 부여
        if (roleObj == null) {
            return UserRole.USER;
        }

        try {
            return UserRole.valueOf(roleObj.toString());
        } catch (IllegalArgumentException e) {
            // "SUPERMAN" 같은 이상한 권한이 들어와도 죽지 않고 USER로 강제 강등!
            return UserRole.USER;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}