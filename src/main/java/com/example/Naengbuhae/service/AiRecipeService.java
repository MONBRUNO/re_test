package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
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

    // 💡 나중에 AI 팀원에게 받을 진짜 주소를 여기에 넣을 예정!
    private final String AI_SERVER_URL = "http://localhost:8000/api/recommend";

    // ✨ 생성자 주입 및 RestTemplateBuilder를 통한 타임아웃(Fail-Safe) 설정
    public AiRecipeService(UserRepository userRepository, 
                           IngredientRepository ingredientRepository, 
                           RestTemplateBuilder restTemplateBuilder) {
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5)) // 서버 연결 자체는 5초 내에 안 되면 포기
                .setReadTimeout(Duration.ofSeconds(30))   // AI가 답변을 고민하는 시간은 최대 30초까지 기다려줌
                .build();
    }

    public AiRecipeResponseDto getAiRecommendation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 유저의 냉장고 식재료 리스트 추출
        List<String> myIngredients = ingredientRepository.findByUser(user).stream()
                .map(Ingredient::getName)
                .collect(Collectors.toList());

        // 냉장고가 비어있을 경우의 방어 로직 (FastAPI 쪽 에러 500 방어)
        if (myIngredients.isEmpty()) {
            throw new IllegalArgumentException("냉장고가 비어있어 AI 추천을 받을 수 없습니다. 식재료를 먼저 추가해주세요!");
        }

        log.info("[AI Recipe] {} 님의 식재료 {}개로 AI 추천 요청 시작...", username, myIngredients.size());

        try {
            // ✨ AI 서버로 데이터 전송 및 결과 수신 (나중에 AI 서버가 완성되면 주석 해제!)
            // return restTemplate.postForObject(AI_SERVER_URL, myIngredients, AiRecipeResponseDto.class);
            
            // 테스트를 위한 임시 가짜 데이터 반환 (통신 뼈대 확인용)
            return new AiRecipeResponseDto(); 
            
        } catch (RestClientException e) {
            // ✨ AI 서버가 죽어있거나 30초 넘게 대답이 없을 때의 방어막
            log.error("[AI Recipe Error] AI 서버 통신 실패: {}", e.getMessage());
            throw new RuntimeException("현재 AI 추천 서버가 혼잡하거나 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
