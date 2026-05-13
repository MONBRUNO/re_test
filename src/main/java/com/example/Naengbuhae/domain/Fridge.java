package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 냉장고 단위로 식재료를 묶는다. 한 사용자가 여러 냉장고를 소유할 수 있고,
// FridgeMember를 통해 다른 사용자에게 공유 가능.
@Entity
@Table(name = "fridges")
@Getter
@Setter
@NoArgsConstructor
public class Fridge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // 예: "우리집 냉장고", "김치냉장고"

    // 냉장고를 만든 사람. 멤버 관리(초대 코드 발급/멤버 제거)는 owner만 가능.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Fridge(User owner, String name) {
        this.owner = owner;
        this.name = name;
    }
}
