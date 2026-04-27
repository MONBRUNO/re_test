package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RecipeRequestDto {
    private String title;
    private String instructions;
    private Integer cookingTime;

    // DTO를 실제 DB 저장용 엔티티로 변환할 때 작성자(User)를 함께 연결
    public Recipe toEntity(User user) {
        return new Recipe(user, title, instructions, cookingTime);
    }
}
