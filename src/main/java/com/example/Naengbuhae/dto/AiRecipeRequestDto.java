package com.example.Naengbuhae.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 서버로 레시피 추천을 요청할 때 사용하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecipeRequestDto {
    private String user_id;
    private String user_preference;
    private List<String> ingredients;
}
