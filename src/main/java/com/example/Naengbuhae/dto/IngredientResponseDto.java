package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Storage;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class IngredientResponseDto {
    private Long id;
    private String name;
    // 기존: private Integer quantity;
    private Double quantity;
    private LocalDate expirationDate;
    private Category category;   // @JsonValue 덕에 응답 JSON에는 한글 라벨로 직렬화됨
    private String unit;
    private Storage storage;     // 동일
    private LocalDate purchaseDate;

    // 생성자: "DB에서 꺼낸 진짜 식재료(Entity)를 주면, 내가 택배 상자(DTO)에 예쁘게 옮겨 담을게!"
    public IngredientResponseDto(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.expirationDate = ingredient.getExpirationDate();
        this.category = ingredient.getCategory();
        this.unit = ingredient.getUnit();
        this.storage = ingredient.getStorage();
        this.purchaseDate = ingredient.getPurchaseDate();
    }
}
