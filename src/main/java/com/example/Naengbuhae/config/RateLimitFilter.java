package com.example.Naengbuhae.config;

import com.example.Naengbuhae.util.ClientIpUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // path → 분당 허용 횟수 (capacity = 분당 허용 횟수, refill = 1분에 capacity만큼 회복)
    private static final Map<String, Bandwidth> LIMITS = Map.of(
            "/user/login",         Bandwidth.builder()
                    .capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build(),
            "/user/signup",        Bandwidth.builder()
                    .capacity(5).refillIntervally(5, Duration.ofMinutes(1)).build(),
            "/user/token/refresh", Bandwidth.builder()
                    .capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build()
    );

    // ✨ 시니어급 방어막: 무한정 쌓이는 Map 대신 똑똑한 Caffeine Cache 도입!
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(10000) // 최대 1만 개의 IP+URI 조합만 기억 (메모리 터짐 방지)
            .expireAfterAccess(Duration.ofMinutes(10)) // 10분 동안 다시 안 찌르면 메모리에서 자동 삭제!
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Bandwidth limit = LIMITS.get(request.getRequestURI());
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClientIpUtil.getClientIp(request);
        String key = ip + ":" + request.getRequestURI();
        
        // ✨ Caffeine Cache 문법으로 변경 (computeIfAbsent -> get)
        Bucket bucket = buckets.get(key, k -> Bucket.builder().addLimit(limit).build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("[RateLimit] 차단 ip={} uri={}", ip, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
        }
    }
}
