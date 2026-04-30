package com.example.Naengbuhae.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {
    private String username;
    private String password;
    private String name;
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private Integer height;
    private Integer weight;
    private String email;
    private String activityLevel;
    private String dietGoal;
    private String allergies;
}
