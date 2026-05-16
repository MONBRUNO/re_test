package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.Ingredient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AiRecipeService {

    private final UserRepository userRepository;
    private final FridgeRepository fridgeRepository;
    private final IngredientRepository ingredientRepository;
    private final RestTemplate restTemplate;

    @Value("${ai.server.url}")
    private String aiServerUrl;

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
    public AiRecipeResponseDto getAiRecommendation(String username) {
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
                return new AiRecipeResponseDto();
            }

            List<String> ingredientNames = ingredients.stream()
                    .map(Ingredient::getName)
                    .toList();

            log.info("[AI Service] 📦 AI 서버로 전송할 식재료 리스트: {}", ingredientNames);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("ingredients", ingredientNames);

            // 외부 AI 서버로 POST 요청 전송
            AiRecipeResponseDto aiResponse = restTemplate.postForObject(aiServerUrl, requestBody, AiRecipeResponseDto.class);

            if (aiResponse != null) {
                log.info("[AI Service] ✅ AI 서버로부터 성공적으로 레시피를 추천받았습니다: {}", aiResponse.getDish_name());
                return aiResponse;
            }

        } catch (RestClientException e) {
            log.error("[AI Service] ❌ AI 서버({}) 통신 중 장애 발생: {}", aiServerUrl, e.getMessage());
        } catch (Exception e) {
            log.error("[AI Service] ❌ 오류 발생: {}", e.getMessage());
        }

        return new AiRecipeResponseDto();
    }
}
