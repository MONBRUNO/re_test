package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Nutrition;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class NutritionDto {

    @PositiveOrZero
    private Integer calories;

    @PositiveOrZero
    private Integer protein;

    @PositiveOrZero
    private Integer carbs;

    @PositiveOrZero
    private Integer fat;

    @PositiveOrZero
    private Integer sodium;

    public NutritionDto(Nutrition n) {
        if (n == null) return;
        this.calories = n.getCalories();
        this.protein = n.getProtein();
        this.carbs = n.getCarbs();
        this.fat = n.getFat();
        this.sodium = n.getSodium();
    }

    public Nutrition toEntity() {
        return new Nutrition(calories, protein, carbs, fat, sodium);
    }
}
