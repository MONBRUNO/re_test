package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.FcmToken;
import com.example.Naengbuhae.repository.FcmTokenRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 앱에서 받은 FCM 디바이스 토큰을 등록/해제.
// 같은 토큰이 다른 유저로 재등록되는 경우(기기 양도 등) 옛 매핑은 새 매핑으로 덮어씀.
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(String username, String token, FcmToken.Platform platform) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("FCM 토큰이 필요합니다.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 같은 token이 이미 있으면 user 매핑만 갱신 — 디바이스 양도 등으로 유저가 바뀐 경우 대응
        fcmTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> {
                    existing.setUser(user);
                    existing.setPlatform(platform);
                },
                () -> fcmTokenRepository.save(new FcmToken(user, token, platform))
        );
    }

    @Transactional
    public void unregister(String token) {
        if (token == null || token.isBlank()) return;
        fcmTokenRepository.deleteByToken(token);
    }
}
