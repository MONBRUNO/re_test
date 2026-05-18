package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Category;
import com.example.Naengbuhae.domain.Ingredient;
import com.example.Naengbuhae.domain.Storage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class IngredientResponseDto {
    private final Long id;
    private final String name;
    private final Double quantity;
    private final LocalDate expirationDate;
    private final Category category;
    private final String unit;
    private final Storage storage;
    private final LocalDate purchaseDate;
    private final UUID fridgeId;
    private final String addedBy;

    // 사용자 알레르기와 매칭된 키워드. 비어있으면 안전. 서비스 계층에서 채워줌.
    @Setter
    private List<String> allergyWarnings = Collections.emptyList();

    public IngredientResponseDto(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.expirationDate = ingredient.getExpirationDate();
        this.category = ingredient.getCategory();
        this.unit = ingredient.getUnit();
        this.storage = ingredient.getStorage();
        this.purchaseDate = ingredient.getPurchaseDate();
        this.fridgeId = ingredient.getFridge() != null ? ingredient.getFridge().getId() : null;
        this.addedBy = ingredient.getUser() != null ? ingredient.getUser().getUsername() : null;
    }
}
