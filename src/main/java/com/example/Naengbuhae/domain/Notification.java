package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 인앱 알림 히스토리.
// FCM 발송과 별개로 DB에 영속화 — 사용자가 푸시를 놓쳤거나 FCM이 비활성화돼도 나중에 볼 수 있게.
// route 필드는 앱 탭 라우팅에 사용 (NotificationRouter 키와 동일).
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_notifications_user_read", columnList = "user_id, is_read")
        })
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    // 앱이 알림 탭 시 어디로 보낼지 — "fridge" / "ingredients" 등. null이면 라우팅 안 함.
    @Column(length = 30)
    private String route;

    // 컬럼명을 RESERVED 키워드 우회용으로 명시
    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification(User user, String title, String body, String route) {
        this.user = user;
        this.title = title;
        this.body = body;
        this.route = route;
    }
}
