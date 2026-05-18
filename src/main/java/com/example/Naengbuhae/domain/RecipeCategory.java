package com.example.Naengbuhae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// 레시피 분류. JSON 직렬화는 한글 라벨, DB 저장은 enum 이름(영어).
// 시드의 5개 카테고리("밥/면","반찬","샐러드","간식","음료") + ETC fallback.
public enum RecipeCategory {
    MAIN("밥/면"),
    SIDE("반찬"),
    SALAD("샐러드"),
    SNACK("간식"),
    DRINK("음료"),
    ETC("기타");

    private final String label;

    RecipeCategory(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static RecipeCategory fromLabel(String value) {
        if (value == null) {
            return null;
        }
        for (RecipeCategory c : values()) {
            if (c.label.equals(value) || c.name().equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("알 수 없는 레시피 분류: " + value);
    }
}
