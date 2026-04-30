package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.Recipe;
import com.example.Naengbuhae.user.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RecipeRequestDto {

    @NotBlank(message = "레시피 제목은 필수 입력 항목입니다.")
    private String title;

    @NotBlank(message = "조리 방법은 필수 입력 항목입니다.")
    private String instructions;

    @NotNull(message = "조리 시간은 필수 입력 항목입니다.")
    @Min(value = 1, message = "조리 시간은 최소 1분 이상이어야 합니다.")
    private Integer cookingTime;

    // DTO를 실제 DB 저장용 엔티티로 변환할 때 작성자(User)를 함께 연결
    public Recipe toEntity(User user) {
        return new Recipe(user, title, instructions, cookingTime);
    }
}
