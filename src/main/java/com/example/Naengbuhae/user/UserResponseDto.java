package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.enums.ActivityLevel;
import com.example.Naengbuhae.domain.enums.DietGoal;
import com.example.Naengbuhae.domain.enums.Gender;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class UserResponseDto {
    private final Long id;
    private final String username;
    private final String name;
    private final Gender gender;
    private final Double height;
    private final Double weight;
    private final LocalDate birthDate;
    private final String email;
    private final ActivityLevel activityLevel;
    private final DietGoal dietGoal;
    private final String allergies;
    private final UserRole role;
    private final Integer recommendedCalories;
    private final boolean emailVerified;
    // LOCAL/KAKAO/NAVER/GOOGLE — 클라이언트가 "비밀번호 변경" 등 LOCAL 전용 UI를 가릴 때 사용.
    private final OAuthProvider provider;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.gender = user.getGender();
        this.height = user.getHeight();
        this.weight = user.getWeight();
        this.birthDate = user.getBirthDate();
        this.email = user.getEmail();
        this.activityLevel = user.getActivityLevel();
        this.dietGoal = user.getDietGoal();
        this.allergies = user.getAllergies();
        this.role = user.getRole();
        this.recommendedCalories = user.getRecommendedCalories();
        this.emailVerified = user.isEmailVerified();
        this.provider = user.getProvider();
    }
}
