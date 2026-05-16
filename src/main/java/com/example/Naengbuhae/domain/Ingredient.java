package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 등록한 사람

    // 어느 냉장고에 속한 식재료인지. 가족 공유 시 같은 fridge_id를 가진 멤버들이 함께 본다.
    // 마이그레이션 직후엔 null인 row가 있을 수 있어 일단 nullable로 두고, 마이그레이션 끝나면 채워짐.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fridge_id", columnDefinition = "UUID")
    private Fridge fridge;

    @Column(nullable = false)
    private String name; // 식재료 이름 (예: 계란)

    // 기존: private Integer quantity;
    private Double quantity; // 수량 (소수점 가능, 예: 1.5)

    private LocalDate expirationDate; // 유통기한 (예: 2026-04-15)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category; // 분류

    @Column(nullable = false)
    private String unit; // 단위 (예: 개, g, kg) — 자유 텍스트

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Storage storage; // 보관 방법

    @Column(nullable = false)
    private LocalDate purchaseDate; // 구매일 (예: 2026-04-15)

    public Ingredient(User user, String name, Double quantity, LocalDate expirationDate,
                      Category category, String unit, Storage storage, LocalDate purchaseDate) {
        this.user = user;
        this.name = name;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
        this.category = category;
        this.unit = unit;
        this.storage = storage;
        this.purchaseDate = purchaseDate;
    }
}
