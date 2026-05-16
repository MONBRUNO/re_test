package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
public class FoodSafetyScheduler {

    @Value("${foodsafety.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    // ✨ 1. DB 창고지기(Repository) 섭외
    private final IngredientRepository ingredientRepository;

    // ✨ 2. 생성자에 창고지기 의존성 주입
    public FoodSafetyScheduler(IngredientRepository ingredientRepository) {
        this.restTemplate = new RestTemplate();
        this.ingredientRepository = ingredientRepository;
    }

    // (실무 적용 시 cron = "0 0 3 * * *" 로 변경하여 매일 새벽 3시에만 돌게 설정)
    @Scheduled(cron = "0/10 * * * * *")
    public void checkRecalledFoods() {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        log.info("[Scheduler] 🚨 식약처 회수/판매중지 식품 API 호출 시작...");

        try {
            // 💡 실전: 최신 데이터 100개를 가져오도록 주소 변경 (1/100)
            String url = "http://openapi.foodsafetykorea.go.kr/api/" + apiKey + "/I0490/json/1/100";
            
            // ✨ JsonNode로 직접 받아 인코딩 무결성 확보
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null) {
                // 💡 API 명세서 분석 적용: I0490 -> row 배열 안에 실제 데이터가 있음!
                JsonNode rows = response.path("I0490").path("row");

                if (!rows.isMissingNode()) {
                    log.info("총 {}개의 회수 대상 식품 데이터를 성공적으로 가져왔습니다!", rows.size());

                    // 💡 배열을 돌면서 위험 식품 추출 및 DB 스캔!
                    for (JsonNode row : rows) {
                        String productName = row.path("PRDTNM").asText(); // 제품명
                        String companyName = row.path("BSSHNM").asText(); // 제조업체명
                        String reason = row.path("RTRVLPRVNS").asText();  // 회수사유

                        if (productName.isBlank()) continue;

                        // ✨ 3. 스캐너 작동! 식약처 제품명이 포함된 식재료가 우리 DB에 있는지 검색
                        List<Ingredient> matchedIngredients = ingredientRepository.findByNameContaining(productName);

                        // 🚨 일치하는 식재료(위험 식품)가 유저의 냉장고에 존재한다면?!
                        if (!matchedIngredients.isEmpty()) {
                            for (Ingredient ingredient : matchedIngredients) {
                                String targetUser = ingredient.getUser().getUsername();
                                
                                log.warn("=======================================================");
                                log.warn("🚨 [초긴급] 유저 '{}'의 냉장고에서 위험 식품 발견!!!", targetUser);
                                log.warn("👉 등록된 이름: {}", ingredient.getName());
                                log.warn("👉 식약처 적발 제품: {} ({})", productName, companyName);
                                log.warn("👉 회수 사유: {}", reason);
                                log.warn("=======================================================");

                                // TODO: 다음 스텝에서 여기에 NotificationService 연동을 진행할 예정입니다!
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Scheduler] 식약처 API 통신 및 대조 중 에러 발생: {}", e.getMessage());
        }
    }
}
