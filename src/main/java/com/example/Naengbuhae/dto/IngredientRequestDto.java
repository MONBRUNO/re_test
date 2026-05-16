package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Storage;
import com.example.Naengbuhae.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class IngredientRequestDto {

    @NotBlank(message = "식재료 이름은 필수입니다!")
    @Size(max = 50, message = "식재료 이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "수량은 필수 입력값입니다!")
    @Positive(message = "수량은 0보다 커야 합니다!")
    private Double quantity;

    @NotNull(message = "유통기한은 필수 입력값입니다!")
    @FutureOrPresent(message = "유통기한은 오늘 또는 미래의 날짜여야 합니다!")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @NotNull(message = "분류는 필수입니다!")
    private Category category;

    @NotBlank(message = "단위는 필수입니다!")
    @Size(max = 20, message = "단위는 20자 이내여야 합니다.")
    private String unit;

    @NotNull(message = "보관 방법은 필수입니다!")
    private Storage storage;

    @NotNull(message = "구매일은 필수 입력값입니다!")
    @PastOrPresent(message = "구매일은 오늘 또는 과거의 날짜여야 합니다!")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    // 어느 냉장고에 추가할지. 없으면 서비스 계층에서 사용자의 기본 냉장고로 자동 선택.
    private UUID fridgeId;

    public Ingredient toEntity(User user) {
        return new Ingredient(user, name, quantity, expirationDate, category, unit, storage, purchaseDate);
    }
}
