package com.example.Naengbuhae.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

// 보관 방법. JSON 직렬화는 한글 라벨, DB 저장은 enum 이름(영어).
public enum Storage {
    REFRIGERATED("냉장"),
    FROZEN("냉동"),
    ROOM("실온");

    private final String label;

    Storage(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Storage fromLabel(String value) {
        if (value == null) {
            throw new IllegalArgumentException("보관 방법이 비어있습니다.");
        }
        for (Storage s : values()) {
            if (s.label.equals(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("알 수 없는 보관 방법: " + value);
    }
}
