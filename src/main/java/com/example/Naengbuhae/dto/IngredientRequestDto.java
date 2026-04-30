package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.user.User;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class IngredientRequestDto {

    @NotBlank(message = "식재료 이름은 필수입니다!")
    private String name;

    @NotNull(message = "수량은 필수 입력값입니다!")
    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다!")
    private Integer quantity;

    @NotNull(message = "유통기한은 필수 입력값입니다!")
    @FutureOrPresent(message = "유통기한은 오늘 또는 미래의 날짜여야 합니다!")
    private LocalDate expirationDate;

    @NotBlank(message = "분류는 필수입니다!")
    private String category;

    @NotBlank(message = "단위는 필수입니다!")
    private String unit;

    @NotBlank(message = "보관 방법은 필수입니다!")
    private String storage;

    @NotNull(message = "구매일은 필수 입력값입니다!")
    @PastOrPresent(message = "구매일은 오늘 또는 과거의 날짜여야 합니다!")
    private LocalDate purchaseDate;

    // 편의 기능: "이 택배 상자(DTO)에 든 내용물을 실제 DB용 식재료(Entity)로 변환해 줘!"
    public Ingredient toEntity(User user) {
        return new Ingredient(user, name, quantity, expirationDate, category, unit, storage, purchaseDate);
    }
}
