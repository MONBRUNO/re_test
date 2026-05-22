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

    // 즉시 실행 벌크 DELETE — 탈퇴 트랜잭션에서 큐 유실 없이 확실히 삭제한다.
    @Modifying
    @Query("delete from UserToken t where t.user = :user")
    void deleteByUser(@Param("user") User user);
}
