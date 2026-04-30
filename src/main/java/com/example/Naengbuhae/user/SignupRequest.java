package com.example.Naengbuhae.user;

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
    private Double height;
    private Double weight;
    private LocalDate birthDate;
    private String email;
    private String activityLevel;
    private String dietGoal;
    private String allergies;
}