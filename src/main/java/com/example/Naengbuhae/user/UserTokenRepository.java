package com.example.Naengbuhae.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenAndType(String token, UserToken.Type type);

    // 같은 사용자의 동일 타입 옛 토큰들을 무효화 (used=true). 재요청 시 호출.
    @Modifying
    @Query("UPDATE UserToken t SET t.used = true " +
           "WHERE t.user = :user AND t.type = :type AND t.used = false")
    void invalidateOlder(@Param("user") User user, @Param("type") UserToken.Type type);

    void deleteByUser(User user);
}
