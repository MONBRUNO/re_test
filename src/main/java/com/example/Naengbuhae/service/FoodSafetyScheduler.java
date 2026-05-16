package com.example.Naengbuhae.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class FoodSafetyScheduler {

    @Value("${foodsafety.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public FoodSafetyScheduler() {
        // ✨ 복잡한 컨버터 설정 없이 순정 RestTemplate 사용!
        // Jackson이 HTTP 응답 바이트를 분석하여 인코딩 문제를 자동으로 해결해줍니다.
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(cron = "0/10 * * * * *")
    public void checkRecalledFoods() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Scheduler] 식약처 API 키가 설정되지 않았습니다. .env의 FOOD_SAFETY_API_KEY를 확인해주세요.");
            return;
        }

        log.info("[Scheduler] 🚨 식약처 회수/판매중지 식품 API 호출 시작...");

        try {
            // 식약처 API 주소 조립 (I0490: 회수판매중지 서비스 코드, 1~5번 데이터만 가져오기)
            String url = "http://openapi.foodsafetykorea.go.kr/api/" + apiKey + "/I0490/json/1/5";
            
            // ✨ 핵심: String.class가 아니라 JsonNode.class로 바로 받아 인코딩 충돌 방지!
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            
            // 예쁘게 정렬해서 터미널에 출력 (toPrettyString)
            if (response != null) {
                log.info("[식약처 응답 데이터 완벽 복구] : \n{}", response.toPrettyString());
            }

        } catch (Exception e) {
            log.error("[Scheduler] 식약처 API 호출 중 에러 발생: {}", e.getMessage());
        }
    }
}
