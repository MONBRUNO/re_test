package com.example.Naengbuhae.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// 사용자의 알레르기 텍스트("땅콩, 갑각류, 우유")와 식재료 이름 리스트를 비교해
// 매칭된 알레르기 키워드를 반환. substring 매칭이라 "땅콩"이 "땅콩버터"도 잡아냄.
//
// 한계: 카테고리 사전이 없어 "우유 알레르기 → 치즈/요거트도 위험" 같은 추론은 못함.
// 1차 구현은 사용자가 키워드를 잘 적는 것을 전제로 함.
public final class AllergyMatcher {

    private AllergyMatcher() {}

    // "땅콩, 갑각류 / 우유 ;계란" 같은 자유 텍스트 → {"땅콩","갑각류","우유","계란"}
    // 콤마/세미콜론/슬래시/공백을 모두 구분자로 취급. 공백/빈 항목 제거. lowercase.
    public static Set<String> parseAllergens(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<>();
        for (String token : raw.split("[\\s,;/]+")) {
            String trimmed = token.trim().toLowerCase();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    // 알레르기 키워드 중 ingredientNames 안에 부분 일치하는 것들 반환.
    // 반환값은 "원본 알레르기 키워드"(사용자 입력 그대로 lowercase) — 어떤 알레르기에 걸렸는지 표시용.
    // 매칭 대상이 없으면 빈 Set.
    public static Set<String> findMatches(Set<String> allergens, Collection<String> ingredientNames) {
        if (allergens == null || allergens.isEmpty() || ingredientNames == null || ingredientNames.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> matched = new LinkedHashSet<>();
        for (String ingName : ingredientNames) {
            if (ingName == null) continue;
            String normalized = ingName.toLowerCase();
            for (String allergen : allergens) {
                if (normalized.contains(allergen) || allergen.contains(normalized)) {
                    matched.add(allergen);
                }
            }
        }
        return matched;
    }
}
