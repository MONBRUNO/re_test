package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import lombok.Getter;

@Getter
public class RecipeResponseDto {

    private Long id;
    private String title;
    private String instructions;
    private Integer cookingTime;
    private String username; // 작성자 이름 추가

    // DB에서 꺼낸 엔티티를 이 DTO 상자에 예쁘게 포장!
    public RecipeResponseDto(Recipe recipe) {
        this.id = recipe.getId();
        this.title = recipe.getTitle();
        this.instructions = recipe.getInstructions();
        this.cookingTime = recipe.getCookingTime();
        this.username = recipe.getUser().getUsername();
    }
}
