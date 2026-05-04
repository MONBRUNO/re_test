package com.example.Naengbuhae.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
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

    @Setter
    private Integer recommendedCalories;

    // 어떤 경로로 가입했는지 (LOCAL/KAKAO/NAVER/GOOGLE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider = OAuthProvider.LOCAL;

    // OAuth 제공자가 부여한 고유 ID (LOCAL은 null)
    private String providerId;

    protected User() {
    }

    // 일반 회원가입용 생성자
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
        this.provider = OAuthProvider.LOCAL;
    }

    // OAuth 신규 가입용 생성자 (신체정보 등은 추가 정보 입력 페이지에서 채움)
    public User(String username, String dummyEncodedPassword, UserRole role, String name, String email,
                OAuthProvider provider, String providerId) {
        this.username = username;
        this.password = dummyEncodedPassword;
        this.role = role;
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
    }

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

    // 기존 LOCAL 사용자가 같은 이메일로 OAuth 로그인하면 provider 정보만 연결 (B-1 정책)
    public void linkOAuth(OAuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    // OAuth 사용자가 추가 정보 입력을 끝냈는지 (필수 필드 다 차있는지)
    public boolean hasCompleteProfile() {
        return gender != null && height != null && weight != null
                && birthDate != null && activityLevel != null && dietGoal != null;
    }
}
