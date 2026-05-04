package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 레시피에 속한 재료인지 연결 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private String name; // 재료 이름

    @Column(nullable = false)
    private Double quantity; // 필요 수량

    private String unit; // 단위

    // 필수 재료 여부 (프론트의 ingredient.required) - 매칭 판정의 핵심
    @Column(nullable = false)
    private boolean required = true;

    public RecipeIngredient(Recipe recipe, String name, Double quantity, String unit, boolean required) {
        this.recipe = recipe;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.required = required;
    }
}
