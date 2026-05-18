package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Nutrition;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class NutritionDto {

    @PositiveOrZero(message = "칼로리는 0 이상이어야 합니다.")
    private Integer calories;

    @PositiveOrZero(message = "단백질 양은 0 이상이어야 합니다.")
    private Integer protein;

    @PositiveOrZero(message = "탄수화물 양은 0 이상이어야 합니다.")
    private Integer carbs;

    @PositiveOrZero(message = "지방 양은 0 이상이어야 합니다.")
    private Integer fat;

    @PositiveOrZero(message = "나트륨 양은 0 이상이어야 합니다.")
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
