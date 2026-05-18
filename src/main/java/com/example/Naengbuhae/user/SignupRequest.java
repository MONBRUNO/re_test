package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.enums.ActivityLevel;
import com.example.Naengbuhae.domain.enums.DietGoal;
import com.example.Naengbuhae.domain.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]{6,}$",
             message = "아이디는 영문, 숫자 조합 6자 이상이어야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(max = 100, message = "비밀번호는 100자 이내여야 합니다.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내여야 합니다.")
    private String name;

    @NotNull(message = "성별을 선택해주세요.")
    private Gender gender;

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

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 254, message = "이메일은 254자 이내여야 합니다.")
    private String email;

    @NotNull(message = "활동량을 선택해주세요.")
    private ActivityLevel activityLevel;

    @NotNull(message = "식단 목표를 선택해주세요.")
    private DietGoal dietGoal;

    @Size(max = 1000, message = "알레르기 정보는 1000자 이내여야 합니다.")
    private String allergies;
}
