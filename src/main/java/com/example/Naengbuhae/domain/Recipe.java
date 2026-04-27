package com.example.Naengbuhae.domain;

import com.example.Naengbuhae.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 추가: 레시피 주인 연결 (N:1 관계) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 이 레시피를 등록한 사용자

    @Column(nullable = false)
    private String title; // 요리 이름 (예: 계란말이)

    @Column(columnDefinition = "TEXT")
    private String instructions; // 만드는 법 (글자가 길어질 수 있으니 TEXT 타입으로!)

    private Integer cookingTime; // 조리 시간(분 단위, 예: 15)

    // 레시피 생성자 (User를 포함하도록 업데이트)
    public Recipe(User user, String title, String instructions, Integer cookingTime) {
        this.user = user;
        this.title = title;
        this.instructions = instructions;
        this.cookingTime = cookingTime;
    }
}
