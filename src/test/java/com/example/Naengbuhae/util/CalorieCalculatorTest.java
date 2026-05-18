package com.example.Naengbuhae.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 절대값(예: "26세 남 70kg = 2509kcal")으로 검증하면 LocalDate.now() 변동에 따라
// 시간 지나면 깨지므로, 같은 조건에서 한 입력만 바꿨을 때의 "상대적 차이"를 검증.
class CalorieCalculatorTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1995, 5, 15);
    private static final double HEIGHT = 175.0;
    private static final double WEIGHT = 70.0;

    @Nested
    @DisplayName("식단 목표")
    class DietGoal {

        @Test
        @DisplayName("체중 감량은 체중 유지보다 정확히 500 kcal 적음")
        void weightLoss() {
            int maintain = calc("체중 유지");
            int loss = calc("체중 감량");
            assertThat(maintain - loss).isEqualTo(500);
        }

        @Test
        @DisplayName("근육량 증가는 체중 유지보다 정확히 300 kcal 많음")
        void muscleGain() {
            int maintain = calc("체중 유지");
            int gain = calc("근육량 증가");
            assertThat(gain - maintain).isEqualTo(300);
        }

        @Test
        @DisplayName("건강 관리는 체중 유지와 동일 (조정값 0)")
        void healthCare() {
            assertThat(calc("건강 관리")).isEqualTo(calc("체중 유지"));
        }

        @Test
        @DisplayName("미정의 값은 체중 유지로 fallback")
        void unknownDietGoal() {
            assertThat(calc("아무거나")).isEqualTo(calc("체중 유지"));
        }

        private int calc(String dietGoal) {
            return CalorieCalculator.calculateRecommendedCalories(
                    "남", BIRTH_DATE, HEIGHT, WEIGHT, "보통 활동", dietGoal);
        }
    }

    @Nested
    @DisplayName("성별")
    class Gender {

        @Test
        @DisplayName("같은 조건에서 남이 여보다 칼로리 더 많음 (BMR 공식 차이로)")
        void maleHigherThanFemale() {
            int male = calc("남");
            int female = calc("여");
            // BMR 차이: 남(+5) - 여(-161) = 166 cal. activity multiplier 곱해진 후 양수여야 함
            assertThat(male).isGreaterThan(female);
        }

        private int calc(String gender) {
            return CalorieCalculator.calculateRecommendedCalories(
                    gender, BIRTH_DATE, HEIGHT, WEIGHT, "보통 활동", "체중 유지");
        }
    }

    @Nested
    @DisplayName("활동량 multiplier")
    class ActivityLevel {

        @Test
        @DisplayName("활동량이 높을수록 권장 칼로리 증가 (오름차순)")
        void increasingByLevel() {
            int sedentary = calc("거의 움직임 없음");
            int light = calc("가벼운 활동");
            int moderate = calc("보통 활동");
            int active = calc("많은 활동");
            int veryActive = calc("매우 많은 활동");

            assertThat(sedentary).isLessThan(light);
            assertThat(light).isLessThan(moderate);
            assertThat(moderate).isLessThan(active);
            assertThat(active).isLessThan(veryActive);
        }

        @Test
        @DisplayName("미정의 값은 거의 움직임 없음(1.2)으로 fallback")
        void unknownActivityLevel() {
            assertThat(calc("롤하는중")).isEqualTo(calc("거의 움직임 없음"));
        }

        private int calc(String activity) {
            return CalorieCalculator.calculateRecommendedCalories(
                    "남", BIRTH_DATE, HEIGHT, WEIGHT, activity, "체중 유지");
        }
    }

    @Nested
    @DisplayName("정수 반환")
    class Rounding {

        @Test
        @DisplayName("결과는 항상 정수 (Math.round로 반올림)")
        void integerOutput() {
            int result = CalorieCalculator.calculateRecommendedCalories(
                    "남", BIRTH_DATE, HEIGHT, WEIGHT, "보통 활동", "체중 유지");
            // int 타입이라 자동으로 정수. 추가로 합리적 범위인지만 확인 (1000~5000 cal)
            assertThat(result).isBetween(1000, 5000);
        }
    }
}
