package com.example.Naengbuhae.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;        // access token (짧은 만료, Authorization 헤더용)
    private String refreshToken; // refresh token (긴 만료, access 만료 시 재발급용)
}