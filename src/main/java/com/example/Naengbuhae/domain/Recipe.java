package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 이 레시피를 등록한 사용자

    @Column(nullable = false)
    private String name; // 요리 이름 (프론트의 recipe.name)

    private String category; // "밥/면", "반찬", "샐러드" 등

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty; // easy / medium / hard

    private Integer cookingTime; // 분
    private Integer servings;    // 인분
    private String imageUrl;

    // 조리 단계 (순서 보존)
    @ElementCollection
    @CollectionTable(name = "recipe_steps", joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderColumn(name = "step_order")
    @Column(name = "step", columnDefinition = "TEXT")
    private List<String> steps = new ArrayList<>();

    // 영양 정보 (1인분 합산값을 그대로 저장 — 표시 시 servings로 나눔)
    @Embedded
    private Nutrition nutrition;

    // 레시피에 필요한 재료 목록 (1:N) — 레시피가 지워지면 재료도 같이 정리
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    public Recipe(User user, String name, String category, Difficulty difficulty,
                  Integer cookingTime, Integer servings, String imageUrl,
                  List<String> steps, Nutrition nutrition) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.difficulty = difficulty;
        this.cookingTime = cookingTime;
        this.servings = servings;
        this.imageUrl = imageUrl;
        if (steps != null) this.steps = new ArrayList<>(steps);
        this.nutrition = nutrition;
    }

    // 재료 추가 편의 메서드 — 양방향 연관관계 동기화
    public void addIngredient(RecipeIngredient ingredient) {
        this.ingredients.add(ingredient);
        ingredient.setRecipe(this);
    }
}
