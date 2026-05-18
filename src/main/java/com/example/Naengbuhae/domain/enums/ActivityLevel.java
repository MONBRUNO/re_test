package com.example.Naengbuhae.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityLevel {
    LOW("거의 움직임 없음"),
    LIGHT("가벼운 활동"),
    MEDIUM("보통 활동"),
    HIGH("많은 활동"),
    VERY_HIGH("매우 많은 활동");

    @JsonValue
    private final String description;
}
