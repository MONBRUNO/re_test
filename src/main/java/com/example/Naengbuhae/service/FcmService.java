package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.FcmToken;
import com.example.Naengbuhae.repository.FcmTokenRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

// FCM 푸시 전송 + Firebase Admin SDK 초기화.
// app.firebase.service-account-path 가 비어있거나 파일이 없으면 init 실패해도 앱은 정상 기동.
// (실패하면 send 호출은 모두 no-op이 됨 — FCM 미설정 환경에서도 서버는 동작)
@Service
@RequiredArgsConstructor
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    private boolean enabled = false;

    @PostConstruct
    public void init() {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FCM 비활성화: app.firebase.service-account-path 미설정");
            return;
        }
        try {
            Resource resource = resourceLoader.getResource(serviceAccountPath);
            if (!resource.exists()) {
                log.warn("FCM 비활성화: 서비스 계정 파일 없음 ({})", serviceAccountPath);
                return;
            }
            try (InputStream stream = resource.getInputStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                enabled = true;
                log.info("FCM 활성화");
            }
        } catch (Exception e) {
            log.warn("FCM 초기화 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public void sendToUser(String username, String title, String body) {
        if (!enabled) return;
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return;
        sendToTokens(fcmTokenRepository.findByUser(user), title, body);
    }

    @Transactional
    public void sendToUsers(List<User> users, String title, String body) {
        if (!enabled || users.isEmpty()) return;
        sendToTokens(fcmTokenRepository.findByUserIn(users), title, body);
    }

    private void sendToTokens(List<FcmToken> tokens, String title, String body) {
        FirebaseMessaging messaging = FirebaseMessaging.getInstance();
        for (FcmToken t : tokens) {
            Message message = Message.builder()
                    .setToken(t.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            try {
                messaging.send(message);
                t.setLastUsedAt(LocalDateTime.now());
            } catch (FirebaseMessagingException e) {
                String code = e.getMessagingErrorCode() == null
                        ? "UNKNOWN" : e.getMessagingErrorCode().name();
                // 죽은 토큰은 정리
                if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                    fcmTokenRepository.deleteByToken(t.getToken());
                } else {
                    log.warn("FCM 전송 실패 ({}): {}", code, e.getMessage());
                }
            }
        }
    }
}
