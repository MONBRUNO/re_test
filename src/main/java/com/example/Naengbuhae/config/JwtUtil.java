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

    // 토큰 내 권한 정보의 KEY 값
    public static final String AUTHORIZATION_KEY = "auth";

    @Value("${JWT_SECRET_KEY}")
    private String SECRET_KEY;
    private final long EXPIRATION = 1000 * 60 * 60;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username, UserRole role) {
        return Jwts.builder()
                .subject(username)
                .claim(AUTHORIZATION_KEY, role) // 권한 정보 추가
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public UserRole getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        String role = claims.get(AUTHORIZATION_KEY, String.class);
        return UserRole.valueOf(role);
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