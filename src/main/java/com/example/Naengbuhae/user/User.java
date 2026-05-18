package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.enums.ActivityLevel;
import com.example.Naengbuhae.domain.enums.DietGoal;
import com.example.Naengbuhae.domain.enums.Gender;
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

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private Double height;

    private Double weight;

    private LocalDate birthDate;

    @Column(unique = true, nullable = false)
    private String email;

    // 이메일 인증 여부. 가입 직후 false → 인증 메일 클릭 시 true.
    // 로컬 가입자에게만 의미 있고, OAuth 가입자는 자동 true(이미 OAuth 제공자가 검증).
    // columnDefinition: 기존 row에 default false 자동 적용 (마이그레이션 시 NOT NULL 위반 방지).
    @Setter
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private DietGoal dietGoal;

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

    // ✨ 정지 여부 필드 추가
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isBanned = false;

    @Column(name = "failed_attempt", nullable = false)
    private int failedAttempt = 0; // 로그인 실패 횟수

    @Column(name = "lock_time")
    private java.time.LocalDateTime lockTime; // 언제까지 잠겨있는지 기록

    protected User() {
    }

    public void increaseFailedAttempt() {
        this.failedAttempt++;
    }

    public void resetFailedAttempt() {
        this.failedAttempt = 0;
        this.lockTime = null;
    }

    public void lockAccount(java.time.LocalDateTime lockUntil) {
        this.lockTime = lockUntil;
    }

    // ✨ 객체지향의 캡슐화를 지키기 위한 도메인 비즈니스 메서드
    public void changeBanStatus(boolean status) {
        this.isBanned = status;
    }

    // 일반 회원가입용 생성자
    public User(String username, String encodedPassword, UserRole role, String name, Gender gender,
                Double height, Double weight, LocalDate birthDate, String email,
                ActivityLevel activityLevel, DietGoal dietGoal, String allergies) {
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
    public void updateProfile(String name, Gender gender, Double height, Double weight,
                              LocalDate birthDate, ActivityLevel activityLevel, DietGoal dietGoal,
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

    // OAuth 신규 가입 시 제공자가 알려준 부가 정보(성별/생년월일)를 prefill.
    // 사용자가 직접 입력한 값이 있으면 덮어쓰지 않고, 비어있을 때만 채움.
    public void prefillFromOAuth(String genderStr, LocalDate birthDate) {
        if (genderStr != null && this.gender == null) {
            try {
                this.gender = "male".equalsIgnoreCase(genderStr) || "M".equalsIgnoreCase(genderStr) || "남".equals(genderStr)
                        ? Gender.MALE : Gender.FEMALE;
            } catch (Exception e) {
                // 파싱 실패 시 유지
            }
        }
        if (birthDate != null && this.birthDate == null) {
            this.birthDate = birthDate;
        }
    }

    // OAuth 사용자가 추가 정보 입력을 끝냈는지 (필수 필드 다 차있는지)
    public boolean hasCompleteProfile() {
        return gender != null && height != null && weight != null
                && birthDate != null && activityLevel != null && dietGoal != null;
    }
}
