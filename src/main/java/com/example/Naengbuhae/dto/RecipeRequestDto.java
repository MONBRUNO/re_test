package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Difficulty;
import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class RecipeRequestDto {

    @NotBlank(message = "레시피 이름은 필수 입력 항목입니다.")
    private String name;

    private String category;

    private Difficulty difficulty;

    @NotNull(message = "조리 시간은 필수 입력 항목입니다.")
    @Min(value = 1, message = "조리 시간은 최소 1분 이상이어야 합니다.")
    private Integer cookingTime;

    @Min(value = 1, message = "인분 수는 최소 1 이상이어야 합니다.")
    private Integer servings;

    private String imageUrl;

    private List<String> steps = new ArrayList<>();

    @Valid
    private NutritionDto nutrition;

    @Valid
    private List<RecipeIngredientDto> ingredients = new ArrayList<>();

    public Recipe toEntity(User user) {
        return new Recipe(
                user, name, category, difficulty, cookingTime, servings, imageUrl,
                steps, nutrition != null ? nutrition.toEntity() : null
        );
    }
}
