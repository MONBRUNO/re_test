package com.example.Naengbuhae.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DietGoal {
    LOSE_WEIGHT("체중 감량"),
    MAINTAIN("체중 유지"),
    GAIN_MUSCLE("근육량 증가"),
    HEALTH_CARE("건강 관리");

    @JsonValue
    private final String description;
}
