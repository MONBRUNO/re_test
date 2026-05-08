package com.example.Naengbuhae.user;

import com.example.Naengbuhae.config.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// RefreshTokenService의 핵심 분기 (특히 재사용 탐지) 회귀 방지.
// Spring context 없이 Mockito만으로 테스트 → 빠름.
// @Value 필드는 ReflectionTestUtils로 주입.
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    RefreshTokenService service;

    User user;

    @BeforeEach
    void setUp() {
        // application.properties의 default 값과 동일하게 설정
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", 31_536_000_000L); // 365일
        ReflectionTestUtils.setField(service, "reuseGraceSeconds", 30L);

        user = mock(User.class);
        // getId()는 로깅 경로(폐기 토큰 분기)에서만 호출되므로 lenient (strict mode 충돌 방지)
        lenient().when(user.getId()).thenReturn(1L);
    }

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("UUID(하이픈 제거, 32자) 형식의 토큰 생성 + DB 저장")
        void savesNewToken() {
            String result = service.issue(user);

            assertThat(result).isNotBlank();
            assertThat(result).hasSize(32); // UUID without dashes
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("존재하지 않는 토큰 → '유효하지 않은' 에러")
        void unknownToken() {
            when(refreshTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refresh("nope"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은");
        }

        @Test
        @DisplayName("자연 만료된 토큰 → '만료' 에러 + 행 삭제")
        void expiredToken() {
            RefreshToken expired = new RefreshToken("tk", user, LocalDateTime.now().minusDays(1));
            when(refreshTokenRepository.findByToken("tk")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> service.refresh("tk"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("만료");

            verify(refreshTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("폐기된 토큰 + grace 내 재제출 → '유효하지 않은' 만, 사용자 토큰 무효화는 안 됨")
        void revokedWithinGrace() {
            RefreshToken token = new RefreshToken("tk", user, LocalDateTime.now().plusDays(1));
            ReflectionTestUtils.setField(token, "revokedAt", LocalDateTime.now().minusSeconds(5));
            when(refreshTokenRepository.findByToken("tk")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.refresh("tk"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은");

            verify(refreshTokenRepository, never()).deleteByUser(any());
        }

        @Test
        @DisplayName("폐기된 토큰 + grace 초과 재제출 → 재사용 탐지 → 사용자 모든 토큰 무효화 + '세션 무효화' 에러")
        void revokedAfterGrace() {
            RefreshToken token = new RefreshToken("tk", user, LocalDateTime.now().plusDays(1));
            ReflectionTestUtils.setField(token, "revokedAt", LocalDateTime.now().minusMinutes(5));
            when(refreshTokenRepository.findByToken("tk")).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.refresh("tk"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("세션이 무효화");

            verify(refreshTokenRepository).deleteByUser(user);
        }

        @Test
        @DisplayName("정상 토큰 → 옛 토큰 revoke 마킹 + 새 access/refresh 발급")
        void validTokenRotates() {
            RefreshToken active = new RefreshToken("oldtk", user, LocalDateTime.now().plusDays(7));
            when(refreshTokenRepository.findByToken("oldtk")).thenReturn(Optional.of(active));
            when(user.getUsername()).thenReturn("alice");
            when(user.getRole()).thenReturn(UserRole.USER);
            when(jwtUtil.createToken("alice", UserRole.USER)).thenReturn("new-access-jwt");

            RefreshTokenService.TokenPair pair = service.refresh("oldtk");

            assertThat(pair.accessToken()).isEqualTo("new-access-jwt");
            assertThat(pair.refreshToken()).isNotBlank().hasSize(32);
            assertThat(active.isRevoked()).isTrue(); // 옛 토큰 폐기 마킹 확인
            // delete가 아니라 revoke (마킹) — 재사용 탐지를 위해 row 유지
            verify(refreshTokenRepository, never()).delete(active);
            verify(refreshTokenRepository).save(any(RefreshToken.class)); // 새 토큰 저장
        }
    }

    @Nested
    @DisplayName("revoke (단일 토큰 폐기)")
    class Revoke {

        @Test
        @DisplayName("활성 토큰 → revokedAt 마킹 (delete가 아니라)")
        void marksAsRevoked() {
            RefreshToken active = new RefreshToken("tk", user, LocalDateTime.now().plusDays(1));
            when(refreshTokenRepository.findByToken("tk")).thenReturn(Optional.of(active));

            service.revoke("tk");

            assertThat(active.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("이미 폐기된 토큰 → revokedAt 시각 변경 안 함 (idempotent)")
        void alreadyRevokedNoOp() {
            RefreshToken token = new RefreshToken("tk", user, LocalDateTime.now().plusDays(1));
            LocalDateTime originalRevokedAt = LocalDateTime.now().minusHours(1);
            ReflectionTestUtils.setField(token, "revokedAt", originalRevokedAt);
            when(refreshTokenRepository.findByToken("tk")).thenReturn(Optional.of(token));

            service.revoke("tk");

            assertThat(token.getRevokedAt()).isEqualTo(originalRevokedAt);
        }

        @Test
        @DisplayName("존재하지 않는 토큰 → 무시 (예외 없음)")
        void unknownTokenIgnored() {
            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

            // 예외 던지지 않아야 함 (로그아웃 흐름이 깨지면 안 됨)
            service.revoke("does-not-exist");
        }
    }

    @Nested
    @DisplayName("revokeAllForUser (탈퇴 등)")
    class RevokeAllForUser {

        @Test
        @DisplayName("사용자의 모든 refresh token row 삭제")
        void deletesAll() {
            service.revokeAllForUser(user);

            verify(refreshTokenRepository).deleteByUser(user);
        }
    }
}
