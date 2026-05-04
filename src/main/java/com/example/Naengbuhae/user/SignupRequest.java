package com.example.Naengbuhae.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(regexp = "^[a-zA-Z0-9]{6,}$", message = "아이디는 영문, 숫자 조합 6자 이상이어야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "성별은 필수 입력값입니다.")
    @Pattern(regexp = "^(남성|여성)$", message = "성별은 '남성' 또는 '여성'만 입력 가능합니다.")
    private String gender;

    @NotNull(message = "생년월일을 입력해주세요.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotNull(message = "키를 입력해주세요.")
    @Positive(message = "올바른 키를 입력해주세요.")
    private Double height;

    @NotNull(message = "몸무게를 입력해주세요.")
    @Positive(message = "올바른 몸무게를 입력해주세요.")
    private Double weight;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "활동량은 필수 입력값입니다.")
    @Pattern(regexp = "^(거의 움직임 없음|가벼운 활동|보통 활동|많은 활동|매우 많은 활동)$",
            message = "활동량은 지정된 한글 양식으로만 입력 가능합니다.")
    private String activityLevel;

    @NotBlank(message = "식단 목표는 필수 입력값입니다.")
    @Pattern(regexp = "^(체중 감량|체중 유지|근육량 증가|건강 관리)$",
            message = "식단 목표는 지정된 한글 양식으로만 입력 가능합니다.")
    private String dietGoal;

    private String allergies;
}
