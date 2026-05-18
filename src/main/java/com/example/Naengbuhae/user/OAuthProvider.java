package com.example.Naengbuhae.user;

// 어떤 방식으로 가입한 사용자인지 표시
// LOCAL: 일반 회원가입 (username + password)
// KAKAO/NAVER/GOOGLE: 소셜 로그인
public enum OAuthProvider {
    LOCAL, KAKAO, NAVER, GOOGLE
}
