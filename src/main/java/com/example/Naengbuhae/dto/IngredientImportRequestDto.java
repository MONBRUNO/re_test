package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Storage;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// 게스트(비로그인) → 로그인 전환 시 로컬에 쌓인 식재료를 한 번에 옮기기 위한 요청.
// IngredientRequestDto와 거의 같지만 유통기한이 과거여도 받아준다 (이미 지난 식재료도 그대로 이전).
@Getter
@Setter
@NoArgsConstructor
public class IngredientImportRequestDto {

    @NotNull
    @Size(min = 1, max = 500, message = "한 번에 1~500개까지만 옮길 수 있습니다.")
    @Valid
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Item {
        @NotBlank
        @Size(max = 50)
        private String name;

        @NotNull
        @Positive
        private Double quantity;

        // 게스트 환경에선 유통기한이 이미 지난 식재료도 그대로 옮긴다 → @FutureOrPresent 제거.
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expirationDate;

        @NotNull
        private Category category;

        @NotBlank
        @Size(max = 20)
        private String unit;

        @NotNull
        private Storage storage;

        // 마찬가지로 구매일도 그대로.
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate purchaseDate;
    }
}
