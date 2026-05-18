package com.example.Naengbuhae.config;

import com.example.Naengbuhae.user.OAuthProvider;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

// 제공자별로 응답 형식이 다른 사용자 정보를 통일된 형태로 추출.
// 네이버는 동의 항목에 따라 성별/생년월일까지 제공하므로 추가 prefill에 활용.
public class OAuth2UserInfo {
    private final String providerId;
    private final String email;
    private final String name;
    private final OAuthProvider provider;
    private final String gender;       // "남" / "여" / null
    private final LocalDate birthDate; // 출생연도 + 생일 조합, null 가능

    private OAuth2UserInfo(String providerId, String email, String name,
                           OAuthProvider provider, String gender, LocalDate birthDate) {
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public String getProviderId() { return providerId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public OAuthProvider getProvider() { return provider; }
    public String getGender() { return gender; }
    public LocalDate getBirthDate() { return birthDate; }

    @SuppressWarnings("unchecked")
    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> {
                // 카카오 응답:
                // { id: 12345, kakao_account: { email: "...", profile: { nickname: "..." } } }
                // 일반 앱은 이메일 권한을 못 받으므로(비즈 앱 전환 전), 이메일 없으면 placeholder 생성
                String providerId = String.valueOf(attributes.get("id"));
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
                if (email == null || email.isBlank()) {
                    email = "kakao_" + providerId + "@kakao.local";
                }
                String nickname = null;
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    nickname = profile != null ? (String) profile.get("nickname") : null;
                }
                yield new OAuth2UserInfo(providerId, email, nickname, OAuthProvider.KAKAO, null, null);
            }
            case "naver" -> {
                // 네이버 응답: { resultcode, message, response: { id, email, name, gender, birthday, birthyear, ... } }
                // 동의항목에 체크되지 않은 필드는 응답에 안 옴
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                String providerId = response != null ? (String) response.get("id") : null;
                String email = response != null ? (String) response.get("email") : null;
                String name = response != null ? (String) response.get("name") : null;
                String gender = response != null ? mapNaverGender((String) response.get("gender")) : null;
                LocalDate birthDate = response != null
                        ? buildBirthDate((String) response.get("birthyear"), (String) response.get("birthday"))
                        : null;
                yield new OAuth2UserInfo(providerId, email, name, OAuthProvider.NAVER, gender, birthDate);
            }
            case "google" -> {
                // 구글 응답: { sub, email, name, picture, ... }
                String providerId = (String) attributes.get("sub");
                String email = (String) attributes.get("email");
                String name = (String) attributes.get("name");
                yield new OAuth2UserInfo(providerId, email, name, OAuthProvider.GOOGLE, null, null);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }

    // 네이버 gender: "M" 남자, "F" 여자, "U" 미상 → 우리 도메인 값으로 매핑
    private static String mapNaverGender(String naverGender) {
        if (naverGender == null) return null;
        return switch (naverGender) {
            case "M" -> "남";
            case "F" -> "여";
            default -> null; // "U" 또는 알 수 없는 값은 미입력 처리
        };
    }

    // birthyear: "1990", birthday: "10-15" → LocalDate(1990, 10, 15)
    // 둘 중 하나라도 없으면 null (사용자가 ProfileComplete 페이지에서 직접 입력)
    private static LocalDate buildBirthDate(String birthyear, String birthday) {
        if (birthyear == null || birthyear.isBlank() || birthday == null || birthday.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthyear + "-" + birthday);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
