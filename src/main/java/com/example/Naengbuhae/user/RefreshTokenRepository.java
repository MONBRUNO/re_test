package com.example.Naengbuhae.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);

    // 만료된 refresh token을 한 번에 삭제. 반환값은 삭제된 행 수 (로그/모니터링용).
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now")
    int deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}
