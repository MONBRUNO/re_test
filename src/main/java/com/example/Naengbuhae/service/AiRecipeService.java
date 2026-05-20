package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.AiRecipeListResponseDto;
import com.example.Naengbuhae.dto.AiRecipeRequestDto;
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AiRecipeService {

    private final UserRepository userRepository;
    private final FridgeRepository fridgeRepository;
    private final IngredientRepository ingredientRepository;
    private final RestTemplate restTemplate;

    // AI 서버 base URL — endpoint는 /api/recommend (냉장고 식재료 기반 추천)
    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    public AiRecipeService(UserRepository userRepository,
                           FridgeRepository fridgeRepository,
                           IngredientRepository ingredientRepository,
                           RestTemplateBuilder restTemplateBuilder) {
        this.userRepository = userRepository;
        this.fridgeRepository = fridgeRepository;
        this.ingredientRepository = ingredientRepository;
        // ✨ 시니어 디테일: 외부 API 호출 시 타임아웃 설정은 필수 (연결 5초, 읽기 30초)
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 사용자의 냉장고 식재료들을 기반으로 AI에게 레시피 추천을 요청합니다.
     */
    public List<AiRecipeResponseDto> getAiRecommendation(String username) {
        log.info("[AI Service] 🤖 사용자 '{}'의 식재료 기반 AI 레시피 추천 요청 진입", username);

        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 사용자가 속한 첫 번째 냉장고를 기준으로 추천 (추후 확장 가능)
            List<Fridge> fridges = fridgeRepository.findAllForMember(user);
            if (fridges.isEmpty()) {
                throw new IllegalArgumentException("소속된 냉장고가 없어 레시피를 추천할 수 없습니다.");
            }

            Fridge targetFridge = fridges.get(0);
            List<Ingredient> ingredients = ingredientRepository.findByFridge(targetFridge);

            if (ingredients.isEmpty()) {
                log.warn("[AI Service] ⚠️ 냉장고에 식재료가 하나도 없어 AI 요청을 중단합니다.");
                return Collections.emptyList();
            }

            List<String> ingredientNames = ingredients.stream()
                    .map(Ingredient::getName)
                    .toList();

            // 사용자의 취향 정보 가져오기 (DietGoal의 description 활용)
            String userPreference = user.getDietGoal() != null ? user.getDietGoal().getDescription() : "건강 관리";

            // AI 서버 규격에 맞춘 요청 DTO 구성
            AiRecipeRequestDto requestDto = AiRecipeRequestDto.builder()
                    .user_id(username)
                    .user_preference(userPreference)
                    .ingredients(ingredientNames)
                    .build();

            String url = aiServerBaseUrl + "/api/recommend";
            log.info("[AI Service] 📦 AI 서버({})로 요청 전송 중...", url);

            // 외부 AI 서버로 POST 요청 전송 (ListResponseDto로 받음)
            AiRecipeListResponseDto aiResponse = restTemplate.postForObject(url, requestDto, AiRecipeListResponseDto.class);

            if (aiResponse != null && "success".equals(aiResponse.getStatus())) {
                log.info("[AI Service] ✅ AI 서버로부터 {}개의 레시피를 추천받았습니다.", aiResponse.getRecommendations().size());
                return aiResponse.getRecommendations();
            }

        } catch (RestClientException e) {
            log.error("[AI Service] ❌ AI 서버({}) 통신 중 장애 발생: {}", aiServerBaseUrl, e.getMessage());
            throw new RuntimeException("AI 서버와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해주세요.");
        } catch (Exception e) {
            log.error("[AI Service] ❌ 오류 발생: {}", e.getMessage());
            throw e;
        }

        return Collections.emptyList();
    }
}
