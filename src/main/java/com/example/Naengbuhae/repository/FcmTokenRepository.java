package com.example.Naengbuhae.repository;

import com.example.Naengbuhae.domain.FcmToken;
import com.example.Naengbuhae.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUser(User user);

    List<FcmToken> findByUserIn(List<User> users);

    Optional<FcmToken> findByToken(String token);

    void deleteByToken(String token);

    // 즉시 실행 벌크 DELETE — 탈퇴 트랜잭션에서 큐 유실 없이 확실히 삭제한다.
    @Modifying
    @Query("delete from FcmToken f where f.user = :user")
    void deleteByUser(@Param("user") User user);
}
