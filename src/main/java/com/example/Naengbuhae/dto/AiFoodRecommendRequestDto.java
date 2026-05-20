package com.example.Naengbuhae.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

// 프론트(웹/앱)가 단일 식재료 기반 추천을 요청할 때 보내는 DTO.
// /analyze 결과 카드의 "이 재료로 추천 요리" 버튼에서 호출 — food_name/cat/영양은 /analyze 응답에서 그대로 가져와 전달.
// AI 서버(/fdmake)가 snake_case로 받으므로 @JsonProperty로 매핑 (입력 호환). user_id/user_preference는 백엔드가 채움.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFoodRecommendRequestDto {

    @JsonProperty("food_name")
    private String foodName;

    private String cat;

    @JsonProperty("nutrition_data")
    private Map<String, Object> nutritionData;
}
