package com.example.Naengbuhae.config;

import com.example.Naengbuhae.user.OAuthProvider;

import java.util.Map;

// 제공자별로 응답 형식이 다른 사용자 정보를 통일된 형태로 추출
public class OAuth2UserInfo {
    private final String providerId;
    private final String email;
    private final String name;
    private final OAuthProvider provider;

    private OAuth2UserInfo(String providerId, String email, String name, OAuthProvider provider) {
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.provider = provider;
    }

    public String getProviderId() { return providerId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public OAuthProvider getProvider() { return provider; }

    @SuppressWarnings("unchecked")
    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> {
                // 카카오 응답:
                // { id: 12345, kakao_account: { email: "...", profile: { nickname: "..." } } }
                String providerId = String.valueOf(attributes.get("id"));
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
                String nickname = null;
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    nickname = profile != null ? (String) profile.get("nickname") : null;
                }
                yield new OAuth2UserInfo(providerId, email, nickname, OAuthProvider.KAKAO);
            }
            case "naver" -> {
                // 네이버 응답: { resultcode, message, response: { id, email, name, ... } }
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                String providerId = response != null ? (String) response.get("id") : null;
                String email = response != null ? (String) response.get("email") : null;
                String name = response != null ? (String) response.get("name") : null;
                yield new OAuth2UserInfo(providerId, email, name, OAuthProvider.NAVER);
            }
            case "google" -> {
                // 구글 응답: { sub, email, name, picture, ... }
                String providerId = (String) attributes.get("sub");
                String email = (String) attributes.get("email");
                String name = (String) attributes.get("name");
                yield new OAuth2UserInfo(providerId, email, name, OAuthProvider.GOOGLE);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }
}
