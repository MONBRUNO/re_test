package com.example.Naengbuhae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// 프론트의 'easy' | 'medium' | 'hard' 와 1:1 매칭되도록 소문자 그대로 유지
public enum Difficulty {
    easy, medium, hard;

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static Difficulty fromJson(String value) {
        if (value == null) return null;
        return Difficulty.valueOf(value.toLowerCase());
    }
}
