package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 냉장고 내 활동 기록 — 가족 활동 통계 화면에 사용.
// Notification은 "받은 알림" 관점이라 행위자가 빠져있어 통계에 부적합 → 별도 테이블.
// 식재료 추가/삭제만 기록 (수정은 노이즈, 멤버 join/leave는 따로 표시할 필요 없음).
@Entity
@Table(name = "activity_logs",
        indexes = {
                @Index(name = "idx_activity_logs_fridge_created", columnList = "fridge_id, created_at DESC"),
                @Index(name = "idx_activity_logs_fridge_actor", columnList = "fridge_id, actor_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class ActivityLog {

    public enum Action { INGREDIENT_ADDED, INGREDIENT_REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fridge_id", columnDefinition = "UUID", nullable = false)
    private Fridge fridge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Action action;

    @Column(nullable = false, length = 50)
    private String ingredientName;

    // 추가 시점 수량/단위 스냅샷 — 삭제 시에는 0/null 가능
    @Column
    private Double quantity;

    @Column(length = 10)
    private String unit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ActivityLog(Fridge fridge, User actor, Action action,
                       String ingredientName, Double quantity, String unit) {
        this.fridge = fridge;
        this.actor = actor;
        this.action = action;
        this.ingredientName = ingredientName;
        this.quantity = quantity;
        this.unit = unit;
    }
}
