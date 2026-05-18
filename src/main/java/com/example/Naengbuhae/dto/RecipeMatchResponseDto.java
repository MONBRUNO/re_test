package com.example.Naengbuhae.dto;

import lombok.Getter;

import java.util.List;

// 프론트의 RecipeMatch 인터페이스와 동일한 구조
// { recipe, matchRate, missingIngredients[], hasIngredients[] }
@Getter
public class RecipeMatchResponseDto {

    private final RecipeResponseDto recipe;
    private final int matchRate; // 0-100
    private final List<String> hasIngredients;
    private final List<String> missingIngredients;

    public RecipeMatchResponseDto(RecipeResponseDto recipe, int matchRate,
                                  List<String> hasIngredients, List<String> missingIngredients) {
        this.recipe = recipe;
        this.matchRate = matchRate;
        this.hasIngredients = hasIngredients;
        this.missingIngredients = missingIngredients;
    }
}
