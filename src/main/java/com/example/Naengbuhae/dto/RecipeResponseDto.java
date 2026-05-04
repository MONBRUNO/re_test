package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class RecipeResponseDto {

    private Long id;
    private String name;
    private String category;
    private Difficulty difficulty;
    private Integer cookingTime;
    private Integer servings;
    private String imageUrl;
    private List<String> steps;
    private NutritionDto nutrition;
    private String username; // 작성자 이름
    private List<RecipeIngredientDto> ingredients;

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
                .collect(Collectors.toList());
    }
}
