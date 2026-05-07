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

@Getter
@Setter
@NoArgsConstructor
public class IngredientRequestDto {

    @NotBlank(message = "식재료 이름은 필수입니다!")
    private String name;

    // 기존: @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다!")
    // 기존: private Integer quantity;
    @NotNull(message = "수량은 필수 입력값입니다!")
    @Positive(message = "수량은 0보다 커야 합니다!") // 0.5도 허용하기 위해 @Positive 사용
    private Double quantity;

    @NotNull(message = "유통기한은 필수 입력값입니다!")
    @FutureOrPresent(message = "유통기한은 오늘 또는 미래의 날짜여야 합니다!")
    @JsonFormat(pattern = "yyyy-MM-dd") // ✅ 추가: 날짜 형식 엇갈림 방어
    private LocalDate expirationDate;

    // Jackson @JsonCreator가 한글 라벨("채소" 등) → enum으로 자동 변환.
    // 잘못된 값이 오면 IllegalArgumentException → GlobalExceptionHandler가 400으로 응답.
    @NotNull(message = "분류는 필수입니다!")
    private Category category;

    @NotBlank(message = "단위는 필수입니다!")
    private String unit;

    @NotNull(message = "보관 방법은 필수입니다!")
    private Storage storage;

    @NotNull(message = "구매일은 필수 입력값입니다!")
    @PastOrPresent(message = "구매일은 오늘 또는 과거의 날짜여야 합니다!")
    @JsonFormat(pattern = "yyyy-MM-dd") // ✅ 추가: 날짜 형식 엇갈림 방어
    private LocalDate purchaseDate;

    // 편의 기능: "이 택배 상자(DTO)에 든 내용물을 실제 DB용 식재료(Entity)로 변환해 줘!"
    public Ingredient toEntity(User user) {
        return new Ingredient(user, name, quantity, expirationDate, category, unit, storage, purchaseDate);
    }
}