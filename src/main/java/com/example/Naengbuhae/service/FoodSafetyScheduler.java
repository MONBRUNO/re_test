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
            // 💡 실전: 최신 데이터 100개를 가져오도록 주소 변경 (1/100)
            String url = "http://openapi.foodsafetykorea.go.kr/api/" + apiKey + "/I0490/json/1/100";
            
            // ✨ 핵심: String.class가 아니라 JsonNode.class로 바로 받아 인코딩 충돌 방지!
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            
            if (response != null) {
                // 💡 API 명세서 분석 적용: I0490 -> row 배열 안에 실제 데이터가 있음!
                JsonNode rows = response.path("I0490").path("row");

                log.info("총 {}개의 회수 대상 식품 데이터를 성공적으로 가져왔습니다!", rows.size());

                // 💡 배열을 돌면서 위험 식품 이름과 사유 추출하기
                for (JsonNode row : rows) {
                    String productName = row.path("PRDTNM").asText(); // 1번: 제품명
                    String companyName = row.path("BSSHNM").asText(); // 3번: 제조업체명
                    String reason = row.path("RTRVLPRVNS").asText();  // 2번: 회수사유

                    // 나중에는 여기서 우리 DB(Ingredient)를 뒤져서 productName이 포함된 식재료가 있는지 찾을 겁니다!
                    // 지금은 일단 추출이 잘 되는지 확인!
                    log.info("🚨 [위험식품 감지] 제품명: {}, 업체명: {}, 사유: {}", productName, companyName, reason);
                }
            }

        } catch (Exception e) {
            log.error("[Scheduler] 식약처 API 호출 중 에러 발생: {}", e.getMessage());
        }
    }
}
