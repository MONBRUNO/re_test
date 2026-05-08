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

    // 만료된 refresh token + 폐기 후 일정 시간이 지난 토큰을 한 번에 삭제.
    //   - expires_at < now: 자연 만료
    //   - revoked_at < cutoff: 폐기 후 cutoff(예: 24h) 지난 row — 재사용 탐지 윈도우 종료된 것
    // 반환값은 삭제된 행 수 (로그/모니터링용).
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :now or r.revokedAt < :revokedCutoff")
    int deleteExpiredOrOldRevoked(@Param("now") LocalDateTime now,
                                  @Param("revokedCutoff") LocalDateTime revokedCutoff);
}
