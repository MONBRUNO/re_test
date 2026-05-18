package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.domain.RecipeCategory;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class RecipeResponseDto {

    private Long id;
    private String name;
    private RecipeCategory category;
    private Difficulty difficulty;
    private Integer cookingTime;
    private Integer servings;
    private String imageUrl;
    private List<String> steps;
    private NutritionDto nutrition;
    private String username; // 작성자 이름
    private List<RecipeIngredientDto> ingredients;

    // 사용자 알레르기와 매칭된 키워드. 비어있으면 안전. 서비스 계층에서 채워줌.
    @Setter
    private List<String> allergyWarnings = Collections.emptyList();

    // 현재 로그인 사용자가 즐겨찾기로 표시했는지. 서비스 계층에서 채워줌.
    @Setter
    private boolean favorite = false;

    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.name = recipe.getName();
        this.category = recipe.getCategory();
        this.difficulty = recipe.getDifficulty();
        this.cookingTime = recipe.getCookingTime();
        this.servings = recipe.getServings();
        this.imageUrl = recipe.getImageUrl();
        this.steps = recipe.getSteps();
        this.nutrition = recipe.getNutrition() != null ? new NutritionDto(recipe.getNutrition()) : null;
        this.username = recipe.getUser().getUsername();
        this.ingredients = recipe.getIngredients().stream()
                .map(RecipeIngredientDto::new)
                .toList();
    }
}
