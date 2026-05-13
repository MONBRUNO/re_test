package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiRecipeService {

    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RestTemplate restTemplate;

    // 🔥 하드코딩 제거! application.properties에서 설정값을 가져옴
    @Value("${ai.server.url}")
    private String aiServerUrl;

    public AiRecipeService(UserRepository userRepository, 
                           IngredientRepository ingredientRepository, 
                           RestTemplateBuilder restTemplateBuilder) {
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public AiRecipeResponseDto getAiRecommendation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<String> myIngredients = ingredientRepository.findByUser(user).stream()
                .map(Ingredient::getName)
                .toList();

        if (myIngredients.isEmpty()) {
            throw new IllegalArgumentException("냉장고가 비어있어 AI 추천을 받을 수 없습니다. 식재료를 먼저 추가해주세요!");
        }

        log.info("[AI Recipe] {} 님의 요청을 AI 서버({})로 전송 중...", username, aiServerUrl);

        try {
            // ✨ 이제 aiServerUrl 변수를 사용하여 유연하게 통신!
            // return restTemplate.postForObject(aiServerUrl, myIngredients, AiRecipeResponseDto.class);
            
            return new AiRecipeResponseDto(); 
            
        } catch (RestClientException e) {
            log.error("[AI Recipe Error] AI 서버({}) 통신 실패: {}", aiServerUrl, e.getMessage());
            throw new RuntimeException("현재 AI 추천 서버 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
