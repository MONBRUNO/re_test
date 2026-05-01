package com.example.Naengbuhae.util;

import java.time.LocalDate;
import java.time.Period;

public class CalorieCalculator {

    /**
     * 사용자의 정보를 바탕으로 하루 권장 칼로리를 계산합니다.
     */
    public static int calculateRecommendedCalories(
            String gender, LocalDate birthDate, double height, double weight,
            String activityLevel, String dietGoal) {

        // 1. 나이 계산 (생년월일부터 오늘까지의 연도 차이)
        int age = Period.between(birthDate, LocalDate.now()).getYears();

        // 2. 기초 대사량 (BMR) 계산 - Mifflin-St Jeor 공식 적용
        double bmr;
        if ("남".equals(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }

        // 3. 활동 대사량 (TDEE) 계산 - 활동량에 따른 가중치 곱하기
        double activityMultiplier = switch (activityLevel) {
            case "거의 움직임 없음" -> 1.2;
            case "가벼운 활동" -> 1.375;
            case "보통 활동" -> 1.55;
            case "많은 활동" -> 1.725;
            case "매우 많은 활동" -> 1.9;
            default -> 1.2; // 만약의 경우를 대비한 기본값
        };
        double tdee = bmr * activityMultiplier;

        // 4. 식단 목표에 따른 최종 칼로리 조정
        double finalCalories = switch (dietGoal) {
            case "체중 감량" -> tdee - 500;   // 감량은 하루 500kcal 적게
            case "근육량 증가" -> tdee + 300; // 증량은 하루 300kcal 넉넉하게
            default -> tdee;                // 유지 및 건강 관리는 그대로
        };

        // 소수점은 반올림해서 깔끔한 정수로 반환
        return (int) Math.round(finalCalories);
    }
}