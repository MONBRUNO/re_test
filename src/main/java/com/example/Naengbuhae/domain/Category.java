package com.example.Naengbuhae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// 식재료 분류. 자바 코드 안에선 enum 이름(영어)으로 다루고
// JSON 직렬화/역직렬화 시에는 한글 라벨로 변환된다.
// DB에는 @Enumerated(EnumType.STRING)으로 enum 이름이 저장됨.
public enum Category {
    VEGETABLE("채소"),
    MEAT("육류"),
    DAIRY("유제품"),
    GRAIN("곡물"),
    SEAFOOD("해산물"),
    FRUIT("과일"),
    ETC("기타");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Category fromLabel(String value) {
        if (value == null) {
            throw new IllegalArgumentException("분류 값이 비어있습니다.");
        }
        for (Category c : values()) {
            if (c.label.equals(value) || c.name().equalsIgnoreCase(value)) {
                return c;
            }
        }
        throw new IllegalArgumentException("알 수 없는 분류: " + value);
    }
}
