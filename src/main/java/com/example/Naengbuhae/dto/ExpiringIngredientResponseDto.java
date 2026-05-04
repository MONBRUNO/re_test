package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Ingredient;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// 유통기한 임박 알림용 — 기본 식재료 정보 + 남은 일수 + 상태
// 프론트의 calculateDDay/getExpiryStatus와 동일 규칙:
//   danger: daysLeft <= 0
//   warning: 1 ~ 3
//   safe: 4+
@Getter
public class ExpiringIngredientResponseDto {

    public enum Status { danger, warning, safe }

    private final Long id;
    private final String name;
    private final Double quantity;
    private final String unit;
    private final String category;
    private final String storage;
    private final LocalDate expirationDate;
    private final LocalDate purchaseDate;
    private final long daysLeft;
    private final Status status;

    public ExpiringIngredientResponseDto(Ingredient ingredient, LocalDate today) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.quantity = ingredient.getQuantity();
        this.unit = ingredient.getUnit();
        this.category = ingredient.getCategory();
        this.storage = ingredient.getStorage();
        this.expirationDate = ingredient.getExpirationDate();
        this.purchaseDate = ingredient.getPurchaseDate();
        this.daysLeft = ingredient.getExpirationDate() != null
                ? ChronoUnit.DAYS.between(today, ingredient.getExpirationDate())
                : Long.MAX_VALUE;
        this.status = classify(daysLeft);
    }

    private static Status classify(long daysLeft) {
        if (daysLeft <= 0) return Status.danger;
        if (daysLeft <= 3) return Status.warning;
        return Status.safe;
    }
}
