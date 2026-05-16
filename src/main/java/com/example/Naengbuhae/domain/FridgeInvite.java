package com.example.Naengbuhae.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 냉장고 초대 코드. owner가 발급, 다른 사용자가 code를 입력해 그 냉장고에 가입.
// 단순한 6자리 코드. 24시간 유효, 1회만 사용 가능 (used=true가 되면 재사용 불가).
@Entity
@Table(name = "fridge_invites",
        indexes = @Index(name = "idx_fridge_invites_code", columnList = "code", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class FridgeInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fridge_id", columnDefinition = "UUID", nullable = false)
    private Fridge fridge;

    @Column(nullable = false, length = 6, unique = true)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FridgeInvite(Fridge fridge, String code, LocalDateTime expiresAt) {
        this.fridge = fridge;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
