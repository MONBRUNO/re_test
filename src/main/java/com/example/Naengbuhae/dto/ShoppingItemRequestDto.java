package com.example.Naengbuhae.dto;

import com.example.Naengbuhae.domain.ShoppingItem;
import com.example.Naengbuhae.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class ShoppingItemRequestDto {

    @NotBlank(message = "장볼 항목의 이름은 필수입니다.")
    @Size(max = 50, message = "장볼 항목의 이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "수량을 입력해주세요.")
    @Positive(message = "수량은 0보다 커야 합니다.")
    private Double quantity;

    @Size(max = 20, message = "단위는 20자 이내여야 합니다.")
    private String unit; // 단위는 필수가 아님 (예: 대파 1 (단위없음) 가능)

    // DTO -> Entity 변환
    public ShoppingItem toEntity(User user) {
        return new ShoppingItem(user, name, quantity, unit);
    }
}