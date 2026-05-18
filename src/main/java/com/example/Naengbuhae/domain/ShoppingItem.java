package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 장바구니 주인 (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name; // 사야 할 식재료 이름 (예: 대파)

    @Column(nullable = false)
    private Double quantity; // 수량 (소수점 가능, 예: 0.5)

    private String unit; // 단위 (예: 단, 개, g)

    @Column(nullable = false)
    private boolean checked = false; // 마트에서 카트에 담았는지 체크 여부

    public ShoppingItem(User user, String name, Double quantity, String unit) {
        this.user = user;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.checked = false; // 처음 등록할 땐 무조건 체크 해제 상태
    }
}