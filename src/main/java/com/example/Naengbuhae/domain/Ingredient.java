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
    private User user; // 이 식재료의 주인!

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
