package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// 냉장고-사용자 N:M 매핑. 한 냉장고에 여러 사용자가 멤버로 가입.
// owner도 자동으로 멤버에 추가됨 (멤버 = "이 냉장고를 볼 수 있는 사람들" 전체).
@Entity
@Table(
        name = "fridge_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"fridge_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class FridgeMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fridge_id", nullable = false)
    private Fridge fridge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public FridgeMember(Fridge fridge, User user) {
        this.fridge = fridge;
        this.user = user;
    }
}
