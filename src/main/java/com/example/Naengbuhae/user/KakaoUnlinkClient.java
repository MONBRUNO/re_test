package com.example.Naengbuhae.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// 카카오 Admin Key로 사용자 연결을 해제. providerId(카카오 user id)만 있으면 호출 가능해서
// 우리는 사용자 access token을 저장하지 않아도 됨.
//
// best-effort: 호출 실패해도 예외를 던지지 않고 로그만 남긴다. 회원 탈퇴 흐름을 막지 않기 위함.
@Slf4j
@Component
public class KakaoUnlinkClient {

    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    // 카카오 서버 hang 시 탈퇴 트랜잭션이 무한 대기하지 않도록 connect/read timeout을 짧게 건다.
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(3))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.oauth.kakao.admin-key:}")
    private String adminKey;

    public void unlink(String providerId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("[KakaoUnlink] KAKAO_ADMIN_KEY 미설정 — 카카오 연결 해제 스킵 (providerId={})", providerId);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", providerId);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(UNLINK_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[KakaoUnlink] 성공 providerId={} response={}", providerId, response.getBody());
            } else {
                log.warn("[KakaoUnlink] 비정상 응답 providerId={} status={} body={}",
                        providerId, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.warn("[KakaoUnlink] 실패 providerId={} reason={}", providerId, e.getMessage());
        }
    }
}
