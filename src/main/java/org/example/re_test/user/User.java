package org.example.re_test.user;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    protected User() {
    }

    // 회원가입용 생성자
    public User(String username, String encodedPassword) {
        this.username = username;
        this.password = encodedPassword;
    }

    // getter만 허용
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // ❗ 비밀번호 변경 (검증은 Service에서 끝낸 값만 받는다)
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}