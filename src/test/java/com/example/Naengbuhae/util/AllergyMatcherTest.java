package com.example.Naengbuhae.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AllergyMatcherTest {

    @Nested
    @DisplayName("parseAllergens")
    class ParseAllergens {

        @Test
        @DisplayName("null이나 빈 문자열은 빈 Set 반환")
        void emptyInputs() {
            assertThat(AllergyMatcher.parseAllergens(null)).isEmpty();
            assertThat(AllergyMatcher.parseAllergens("")).isEmpty();
            assertThat(AllergyMatcher.parseAllergens("   ")).isEmpty();
        }

        @Test
        @DisplayName("단일 키워드는 lowercase 1개 토큰으로 변환")
        void singleKeyword() {
            assertThat(AllergyMatcher.parseAllergens("땅콩"))
                    .containsExactly("땅콩");
            assertThat(AllergyMatcher.parseAllergens("Peanut"))
                    .containsExactly("peanut");
        }

        @Test
        @DisplayName("콤마/세미콜론/슬래시/공백 모두 구분자로 인식")
        void multipleSeparators() {
            assertThat(AllergyMatcher.parseAllergens("땅콩, 갑각류 / 우유 ;계란"))
                    .containsExactlyInAnyOrder("땅콩", "갑각류", "우유", "계란");
        }

        @Test
        @DisplayName("중복 키워드는 한 번만 포함")
        void deduplicates() {
            assertThat(AllergyMatcher.parseAllergens("땅콩, 땅콩, 땅콩"))
                    .containsExactly("땅콩");
        }

        @Test
        @DisplayName("앞뒤 공백 trim")
        void trims() {
            assertThat(AllergyMatcher.parseAllergens("  땅콩  ,  우유  "))
                    .containsExactlyInAnyOrder("땅콩", "우유");
        }

        @Test
        @DisplayName("연속 구분자(빈 토큰)는 제거")
        void filtersEmptyTokens() {
            assertThat(AllergyMatcher.parseAllergens("땅콩,,우유"))
                    .containsExactlyInAnyOrder("땅콩", "우유");
            assertThat(AllergyMatcher.parseAllergens(",,,땅콩,,,"))
                    .containsExactly("땅콩");
        }
    }

    @Nested
    @DisplayName("findMatches")
    class FindMatches {

        @Test
        @DisplayName("알레르기가 비어있으면 빈 결과")
        void noAllergens() {
            assertThat(AllergyMatcher.findMatches(Set.of(), List.of("땅콩버터"))).isEmpty();
            assertThat(AllergyMatcher.findMatches(null, List.of("땅콩버터"))).isEmpty();
        }

        @Test
        @DisplayName("식재료가 비어있으면 빈 결과")
        void noIngredients() {
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩"), List.of())).isEmpty();
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩"), null)).isEmpty();
        }

        @Test
        @DisplayName("정확히 일치하면 매칭")
        void exactMatch() {
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩"), List.of("땅콩")))
                    .containsExactly("땅콩");
        }

        @Test
        @DisplayName("알레르기가 식재료 이름의 부분일 때 매칭 (땅콩 in 땅콩버터)")
        void allergenIsSubstring() {
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩"), List.of("땅콩버터")))
                    .containsExactly("땅콩");
        }

        @Test
        @DisplayName("식재료가 알레르기의 부분일 때도 매칭 (양방향)")
        void ingredientIsSubstring() {
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩버터"), List.of("땅콩")))
                    .containsExactly("땅콩버터");
        }

        @Test
        @DisplayName("매칭 안 되는 식재료는 결과에 없음")
        void noMatch() {
            assertThat(AllergyMatcher.findMatches(Set.of("땅콩"), List.of("토마토", "양파")))
                    .isEmpty();
        }

        @Test
        @DisplayName("여러 알레르기 중 매칭된 것만 반환")
        void multipleAllergensSomeMatch() {
            assertThat(AllergyMatcher.findMatches(
                    Set.of("땅콩", "우유", "갑각류"),
                    List.of("땅콩버터", "치즈우유", "토마토")))
                    .containsExactlyInAnyOrder("땅콩", "우유");
        }

        @Test
        @DisplayName("같은 알레르기가 여러 식재료에 걸려도 한 번만 반환")
        void deduplicatesAcrossIngredients() {
            assertThat(AllergyMatcher.findMatches(
                    Set.of("땅콩"),
                    List.of("땅콩버터", "땅콩잼", "땅콩가루")))
                    .containsExactly("땅콩");
        }

        @Test
        @DisplayName("대소문자 무시 매칭")
        void caseInsensitive() {
            assertThat(AllergyMatcher.findMatches(
                    Set.of("peanut"),
                    List.of("Peanut Butter")))
                    .containsExactly("peanut");
        }

        @Test
        @DisplayName("null 식재료 항목은 무시")
        void skipsNullIngredients() {
            // ArrayList allows nulls — Hibernate may return such on partial fetches
            java.util.List<String> ingredients = new java.util.ArrayList<>();
            ingredients.add("땅콩");
            ingredients.add(null);
            ingredients.add("우유");

            assertThat(AllergyMatcher.findMatches(Set.of("땅콩", "우유"), ingredients))
                    .containsExactlyInAnyOrder("땅콩", "우유");
        }
    }
}
