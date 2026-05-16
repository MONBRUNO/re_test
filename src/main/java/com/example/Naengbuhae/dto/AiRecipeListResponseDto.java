package com.example.Naengbuhae.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 서버로부터 전달받은 레시피 목록 응답을 담는 DTO
 */
@Getter
@NoArgsConstructor
public class AiRecipeListResponseDto {
    private String status;
    private List<AiRecipeResponseDto> recommendations;
}
