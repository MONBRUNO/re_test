package com.example.Naengbuhae.config;

import com.example.Naengbuhae.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    public static final String AUTHORIZATION_KEY = "auth";

    // 🔥 Hotfix 2: Fallback 키가 properties를 거쳐서 안전하게 들어옴
    @Value("${jwt.secret}")
    private String secretKey;

    // access token 만료시간 (ms). 기본 30분. 테스트할 때만 짧게(예: 60000=1분) 두기.
    @Value("${app.jwt.access-token-expiration-ms:1800000}")
    private long EXPIRATION;

    @jakarta.annotation.PostConstruct
    public void checkDefaultKey() {
        String defaultKey = "this_is_a_very_long_default_secret_key_for_local_development_environment_12345!";
        if (defaultKey.equals(secretKey)) {
            log.error("===============================================================");
            log.error(" [CRITICAL SECURITY WARNING] ");
            log.error("---------------------------------------------------------------");
            log.error(" ⚠️  기본(Default) JWT 시크릿 키가 감지되었습니다!");
            log.error(" 현재 운영 환경의 보안이 매우 취약한 상태일 수 있습니다.");
            log.error("");
            log.error(" 조치 사항:");
            log.error(" 1. .env 파일의 'JWT_SECRET_KEY'를 복잡한 문자열로 변경하세요.");
            log.error(" 2. 변경 후 애플리케이션을 반드시 재시작하세요.");
            log.error("===============================================================");
        } else {
            log.info("[Security] 커스텀 JWT 시크릿 키가 정상적으로 로드되었습니다.");
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
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

    // 🔥 Hotfix 3: 토큰에서 권한 추출 시 예외 흡수 및 USER 강등 로직
    public UserRole getRoleFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            Object roleObj = claims.get(AUTHORIZATION_KEY);

            // 권한 정보가 없으면 기본 USER 권한 부여
            if (roleObj == null || roleObj.toString().trim().isEmpty()) {
                log.warn("[JWT Warning] 토큰에 권한 정보가 없어 USER 권한으로 기본 할당합니다.");
                return UserRole.USER;
            }

            try {
                return UserRole.valueOf(roleObj.toString());
            } catch (IllegalArgumentException e) {
                // "SUPERMAN" 같은 이상한 권한이 들어와도 죽지 않고 USER로 강제 강등!
                log.error("[JWT Error] 알 수 없는 권한 명칭입니다. USER로 강등 조치합니다. 원인: {}", e.getMessage());
                return UserRole.USER;
            }
        } catch (Exception e) {
            // 파싱 중 다른 문제가 생겨도 일단 최소 권한 부여
            log.error("[JWT Error] 권한 파싱 실패. USER로 강등 조치. 원인: {}", e.getMessage());
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