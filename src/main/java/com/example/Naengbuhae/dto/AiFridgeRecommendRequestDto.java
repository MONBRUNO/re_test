package com.example.Naengbuhae.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// 웹/앱 AI 추천 모달에서 사용자가 직접 선택한 식재료/스타일 기반 추천 요청.
// (기존 `getAiRecommendation`은 냉장고 전체 자동 + dietGoal 자동인 GET endpoint와 별개)
//
// styles는 한식/양식/일식 등 다중 선택 가능 → 백엔드가 콤마 join해서 AI 서버 user_preference로 전달.
// ingredients는 사용자가 모달 Step 1에서 체크한 식재료 이름들 — AI 서버 ingredients 그대로 전달.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiFridgeRecommendRequestDto {

    private List<String> ingredients;
    private List<String> styles;
}
