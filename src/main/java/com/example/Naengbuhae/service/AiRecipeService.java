package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.dto.AiRecipeResponseDto;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecipeService {

    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // 💡 나중에 AI 팀원에게 받을 진짜 주소를 여기에 넣을 예정!
    private final String AI_SERVER_URL = "http://localhost:8000/api/recommend";

    public AiRecipeResponseDto getAiRecommendation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 유저의 냉장고 식재료 리스트 추출
        List<String> myIngredients = ingredientRepository.findByUser(user).stream()
                .map(Ingredient::getName)
                .collect(Collectors.toList());

        // AI 서버로 데이터 전송 및 결과 수신 (나중에 AI 서버가 완성되면 활성화)
        // return restTemplate.postForObject(AI_SERVER_URL, myIngredients, AiRecipeResponseDto.class);

        // 일단은 뼈대만 잡는 거니 '가짜 데이터(Mock)'를 반환하도록 설계해둠
        return new AiRecipeResponseDto();
    }
}