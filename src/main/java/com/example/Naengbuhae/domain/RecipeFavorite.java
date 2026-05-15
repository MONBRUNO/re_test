package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 사용자가 마음에 들어 "하트" 누른 레시피 — User <-> Recipe N:M의 join row.
// User-Recipe 한 쌍에 unique constraint로 중복 방지. 토글 시 row 추가/삭제로 처리.
@Entity
@Table(name = "recipe_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recipe_favorite_user_recipe",
                columnNames = {"user_id", "recipe_id"}),
        indexes = @Index(name = "idx_recipe_favorite_user", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public RecipeFavorite(User user, Recipe recipe) {
        this.user = user;
        this.recipe = recipe;
        this.createdAt = LocalDateTime.now();
    }
}
