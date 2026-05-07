package com.example.Naengbuhae.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class AiRecipeResponseDto {
    private String dish_name;               // AI 추천 요리 이름
    private List<String> additional_ingredients; // 추가로 필요한 재료
    private String health_benefits;         // 음식의 효능 및 추천 이유
    private String recipe_tip;              // 간단한 조리 팁
}