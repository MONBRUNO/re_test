package com.example.Naengbuhae.config;

import com.example.Naengbuhae.util.ClientIpUtil;
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
import java.util.concurrent.ConcurrentHashMap;

// 인증 엔드포인트의 IP당 호출 빈도를 제한. Bucket4j의 토큰 버킷 알고리즘.
// 단일 인스턴스 메모리 기반 — 다중 인스턴스로 확장 시 Redis 등 분산 저장소 필요.
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

    // (IP + path) → 해당 사용자/path 전용 bucket. 메모리 누수 방지를 위해 주기 정리는 추후 작업.
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

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
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());

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
