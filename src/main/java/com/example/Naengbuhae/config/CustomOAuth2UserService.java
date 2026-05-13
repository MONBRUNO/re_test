package com.example.Naengbuhae.config;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.user.OAuthProvider;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.example.Naengbuhae.user.UserRole;
import com.example.Naengbuhae.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// OAuth 인증 성공 후 호출. 우리 DB와 매칭/생성하고 우리 User로 변환된 인증 객체를 반환.
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

        // 이메일이 없는 경우(카카오 일반 앱 등) OAuth2UserInfo가 placeholder를 채워주므로 별도 검증 없이 진행
        User user = findOrCreateUser(userInfo);

        // 우리 시스템의 권한과 username을 attributes에 추가 (DefaultOAuth2User는 nameAttributeKey가 필요함)
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(user.getRole().getAuthority())),
                oAuth2User.getAttributes(),
                getNameAttributeKey(registrationId)
        );
    }

    private User findOrCreateUser(OAuth2UserInfo info) {
        // 1. 같은 provider + providerId로 이미 가입한 사용자 → 그대로 사용
        // (UserRepository에 메서드 추가 필요할 수도 있어 여기서는 email 우선 매칭)

        // 2. 같은 이메일로 가입한 사용자 있으면 OAuth 연결 (B-1 정책)
        return userRepository.findByEmail(info.getEmail())
                .map(existing -> {
                    if (existing.getProvider() == OAuthProvider.LOCAL) {
                        existing.linkOAuth(info.getProvider(), info.getProviderId());
                        log.info("[OAuth] 기존 LOCAL 계정({})에 {} 연결", info.getEmail(), info.getProvider());
                    }
                    return existing;
                })
                .orElseGet(() -> createNewUser(info));
    }

    private User createNewUser(OAuth2UserInfo info) {
        // username은 충돌 방지를 위해 provider+providerId로 부여
        String username = info.getProvider().name().toLowerCase() + "_" + info.getProviderId();
        // 비밀번호는 추측 불가능한 더미 (OAuth 사용자는 일반 로그인 막힘)
        String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = new User(
                username,
                dummyPassword,
                UserRole.USER,
                info.getName() != null ? info.getName() : "사용자",
                info.getEmail(),
                info.getProvider(),
                info.getProviderId()
        );
        // 제공자가 알려준 성별/생년월일이 있으면 prefill (네이버는 동의 항목에 따라 제공)
        user.prefillFromOAuth(info.getGender(), info.getBirthDate());
        // OAuth 가입자는 이미 제공자가 이메일 검증했으므로 인증 메일 안 보내고 바로 verified.
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);

        // 신규 OAuth 가입자에게도 "<이름>의 냉장고" 자동 생성
        Fridge defaultFridge = fridgeRepository.save(
                new Fridge(savedUser, UserService.defaultFridgeName(savedUser)));
        fridgeMemberRepository.save(new FridgeMember(defaultFridge, savedUser));

        log.info("[OAuth] 신규 사용자 가입: {} ({}) — gender={}, birthDate={}",
                info.getEmail(), info.getProvider(), info.getGender(), info.getBirthDate());
        return savedUser;
    }

    // SuccessHandler에서 사용자 정보를 다시 꺼낼 때 쓸 키
    private String getNameAttributeKey(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> "id";
            case "naver" -> "response";
            case "google" -> "sub";
            default -> "id";
        };
    }
}
