package com.example.Naengbuhae.controller;

import com.example.Naengbuhae.dto.NotificationResponseDto;
import com.example.Naengbuhae.service.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

// 인앱 알림 센터 API.
// 모든 엔드포인트 로그인 필요 (SecurityConfig의 anyRequest().authenticated() 적용).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final AppNotificationService appNotificationService;

    // 최신 50개. 페이지네이션 필요해지면 그때 확장.
    @GetMapping
    public List<NotificationResponseDto> list(Principal principal) {
        return appNotificationService.listForUser(principal.getName()).stream()
                .map(NotificationResponseDto::new)
                .toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal principal) {
        return Map.of("count", appNotificationService.unreadCount(principal.getName()));
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(Principal principal) {
        int updated = appNotificationService.markAllRead(principal.getName());
        return Map.of("updated", updated);
    }
}
