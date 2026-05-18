package com.example.Naengbuhae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SystemStatsResponseDto {
    private Long totalUsers;
    private Long totalRecipes;
    private Long totalIngredients;
}
