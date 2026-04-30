package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
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
}