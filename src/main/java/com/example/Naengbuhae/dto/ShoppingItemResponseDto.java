package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.ShoppingItem;
import lombok.Getter;

@Getter
public class ShoppingItemResponseDto {
    private Long id;
    private String name;
    private Double quantity;
    private String unit;
    private boolean checked;

    // Entity -> DTO 변환
    public ShoppingItemResponseDto(ShoppingItem item) {
        this.id = item.getId();
        this.name = item.getName();
        this.quantity = item.getQuantity();
        this.unit = item.getUnit();
        this.checked = item.isChecked();
    }
}