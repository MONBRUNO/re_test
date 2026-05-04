package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter; // ✅ 롬복 Setter 임포트 추가!
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private String gender;

    private Double height;

    private Double weight;

    private LocalDate birthDate;

    @Column(unique = true, nullable = false)
    private String email;

    private String activityLevel;

    private String dietGoal;

    @Column(length = 1000)
    private String allergies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // ✅ 길었던 메서드 대신 롬복 어노테이션 딱 하나로 깔끔하게!
    @Setter
    private Integer recommendedCalories; // 하루 권장 칼로리 저장용

    protected User() {
    }

    // 회원가입용 생성자
    public User(String username, String encodedPassword, UserRole role, String name, String gender,
                Double height, Double weight, LocalDate birthDate, String email,
                String activityLevel, String dietGoal, String allergies) {
        this.username = username;
        this.password = encodedPassword;
        this.role = role;
        this.name = name;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.birthDate = birthDate;
        this.email = email;
        this.activityLevel = activityLevel;
        this.dietGoal = dietGoal;
        this.allergies = allergies;
    }

    // 비밀번호 변경 기능
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 프로필 수정 (username/email/password/role은 변경 불가)
    public void updateProfile(String name, String gender, Double height, Double weight,
                              LocalDate birthDate, String activityLevel, String dietGoal,
                              String allergies) {
        this.name = name;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.birthDate = birthDate;
        this.activityLevel = activityLevel;
        this.dietGoal = dietGoal;
        this.allergies = allergies;
    }
}