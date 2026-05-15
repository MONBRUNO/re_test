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
    // 이메일 미인증으로 거부된 경우만 true. 클라가 "메일 다시 받기" UI를 띄울지 분기할 때 사용.
    private boolean needsEmailVerification;
    // 미인증 거부 시 어느 메일로 보냈는지 안내하기 위해 같이 내려준다. 일반 실패는 null.
    private String email;

    // 기존 호출부 호환 — 4-인자 생성자를 추가로 노출.
    public LoginResponse(boolean success, String message, String token, String refreshToken) {
        this(success, message, token, refreshToken, false, null);
    }
}