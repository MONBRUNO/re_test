package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.RecipeIngredient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RecipeIngredientDto {

    @NotBlank(message = "재료 이름은 필수입니다.")
    @Size(max = 50, message = "재료 이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "재료 수량은 필수입니다.")
    @Positive(message = "재료 수량은 0보다 커야 합니다.")
    private Double quantity;

    @Size(max = 20, message = "재료 단위는 20자 이내여야 합니다.")
    private String unit;

    // 필수 재료 여부 — 기본값 true (요청에서 생략 시 필수로 간주)
    private boolean required = true;

    // 응답용 생성자 (엔티티 → DTO)
    public RecipeIngredientDto(RecipeIngredient ingredient) {
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.unit = ingredient.getUnit();
        this.required = ingredient.isRequired();
    }
}
