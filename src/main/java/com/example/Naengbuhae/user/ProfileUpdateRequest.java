package com.example.Naengbuhae.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    private String name;

    @NotBlank(message = "성별은 필수 입력값입니다.")
    @Pattern(regexp = "^(남|여)$", message = "성별은 '남' 또는 '여'만 입력 가능합니다.")
    private String gender;

    @NotNull(message = "생년월일을 입력해주세요.")
    @Past(message = "생년월일은 과거의 날짜여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotNull(message = "키를 입력해주세요.")
    @Positive(message = "올바른 키를 입력해주세요.")
    private Double height;

    @NotNull(message = "몸무게를 입력해주세요.")
    @Positive(message = "올바른 몸무게를 입력해주세요.")
    private Double weight;

    @NotBlank(message = "활동량은 필수 입력값입니다.")
    @Pattern(regexp = "^(거의 움직임 없음|가벼운 활동|보통 활동|많은 활동|매우 많은 활동)$",
            message = "활동량은 지정된 한글 양식으로만 입력 가능합니다.")
    private String activityLevel;

    @NotBlank(message = "식단 목표는 필수 입력값입니다.")
    @Pattern(regexp = "^(체중 감량|체중 유지|근육량 증가|건강 관리)$",
            message = "식단 목표는 지정된 한글 양식으로만 입력 가능합니다.")
    private String dietGoal;

    @Size(max = 1000, message = "알레르기 정보는 1000자 이내여야 합니다.")
    private String allergies;
}
