package com.example.Naengbuhae.service;

import com.example.Naengbuhae.domain.Notification;
import com.example.Naengbuhae.repository.NotificationRepository;
import com.example.Naengbuhae.user.User;
import com.example.Naengbuhae.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 인앱 알림 단일 진입점.
// 1) Notification 행을 DB에 영속화 (히스토리)
// 2) FcmService로 푸시 전송 (FCM 비활성화돼도 1번은 항상 수행됨)
//
// FridgeService / IngredientService 등은 fcmService를 직접 부르지 않고 이걸 사용.
// 다중 수신자는 한 번에 모두 영속화한 뒤 한 번에 FCM 발송.
@Service
@RequiredArgsConstructor
public class AppNotificationService {

    private static final int LIST_PAGE_SIZE = 50;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public void notifyUser(User user, String title, String body, String route) {
        notificationRepository.save(new Notification(user, title, body, route));
        fcmService.sendToUser(user.getUsername(), title, body, route);
    }

    @Transactional
    public void notifyUsers(List<User> users, String title, String body, String route) {
        if (users.isEmpty()) return;
        List<Notification> rows = new ArrayList<>(users.size());
        for (User u : users) {
            rows.add(new Notification(u, title, body, route));
        }
        notificationRepository.saveAll(rows);
        fcmService.sendToUsers(users, title, body, route);
    }

    // === 조회 ===

    @Transactional(readOnly = true)
    public List<Notification> listForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Pageable pageable = PageRequest.of(0, LIST_PAGE_SIZE);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public int markAllRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return notificationRepository.markAllAsRead(user);
    }
}
