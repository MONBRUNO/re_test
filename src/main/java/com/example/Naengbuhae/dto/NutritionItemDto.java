package com.example.Naengbuhae.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// AI 서버(capstone-ai) /analyze 응답의 개별 항목 — 100g 기준 영양정보.
// AI 서버가 snake_case로 주므로 @JsonProperty로 매핑. 직렬화 시 클라이언트에도 동일 키로 나감.
// is_corrected는 "true"/"false" 문자열로 옴 (AI 서버 프롬프트 명세).
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NutritionItemDto {

    @JsonProperty("food_name")
    private String foodName;

    @JsonProperty("is_corrected")
    private String isCorrected;

    private String cat;
    private Double cal;
    private Double carbohydrate;
    private Double protein;
    private Double fat;
}
