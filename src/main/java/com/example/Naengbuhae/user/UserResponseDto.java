package com.example.Naengbuhae.user;

import lombok.Getter;
import java.time.LocalDate;

@Getter
public class UserResponseDto {
    private Long id;
    private String username;
    private String name;
    private String gender;
    private Double height;
    private Double weight;
    private LocalDate birthDate;
    private String email;
    private String activityLevel;
    private String dietGoal;
    private String allergies;
    private UserRole role;
    private Integer recommendedCalories;
    private boolean emailVerified;

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
    }
}
