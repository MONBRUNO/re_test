package com.example.Naengbuhae.user;

import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock IngredientRepository ingredientRepository;
    @Mock RecipeRepository recipeRepository;
    @Mock ShoppingItemRepository shoppingItemRepository;
    @Mock RefreshTokenService refreshTokenService;
    @Mock KakaoUnlinkClient kakaoUnlinkClient;

    @InjectMocks UserService service;

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("아이디 중복 → '이미 존재하는 아이디입니다.' 반환, save 호출 안 됨")
        void duplicateUsername() {
            SignupRequest req = validRequest();
            when(userRepository.findByUsername(req.getUsername()))
                    .thenReturn(Optional.of(mock(User.class)));

            String result = service.signup(req);

            assertThat(result).isEqualTo("이미 존재하는 아이디입니다.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("이메일 중복 → '이미 사용 중인 이메일입니다.'")
        void duplicateEmail() {
            SignupRequest req = validRequest();
            when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(req.getEmail()))
                    .thenReturn(Optional.of(mock(User.class)));

            String result = service.signup(req);

            assertThat(result).isEqualTo("이미 사용 중인 이메일입니다.");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("비밀번호에 한글 포함 → 에러")
        void passwordWithKorean() {
            SignupRequest req = validRequest();
            req.setPassword("비밀번호1!"); // 한글 포함
            when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

            String result = service.signup(req);

            assertThat(result).contains("한글");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("비밀번호 패턴 위반 (특수문자 없음) → 에러")
        void passwordPatternViolation() {
            SignupRequest req = validRequest();
            req.setPassword("password1"); // 특수문자 없음
            when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());

            String result = service.signup(req);

            assertThat(result).contains("8자 이상");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 가입 → '회원가입 성공' + save 호출 (인코딩된 비번)")
        void successfulSignup() {
            SignupRequest req = validRequest();
            when(userRepository.findByUsername(req.getUsername())).thenReturn(Optional.empty());
            when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(req.getPassword())).thenReturn("ENCODED");

            String result = service.signup(req);

            assertThat(result).isEqualTo("회원가입 성공");
            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode(req.getPassword());
        }

        private SignupRequest validRequest() {
            SignupRequest req = new SignupRequest();
            req.setUsername("alice123");
            req.setPassword("ValidPwd1!");
            req.setName("홍길동");
            req.setGender("남");
            req.setBirthDate(LocalDate.of(1995, 5, 15));
            req.setHeight(175.0);
            req.setWeight(70.0);
            req.setEmail("alice@example.com");
            req.setActivityLevel("보통 활동");
            req.setDietGoal("체중 유지");
            req.setAllergies(null);
            return req;
        }
    }

    @Nested
    @DisplayName("deleteMyAccount")
    class DeleteMyAccount {

        @Test
        @DisplayName("사용자 없음 → IllegalArgumentException")
        void userNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteMyAccount("ghost"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("LOCAL 사용자 → cascade delete 다 호출 + Kakao unlink는 호출 안 됨")
        void localUserNoKakaoUnlink() {
            User user = mock(User.class);
            lenient().when(user.getProvider()).thenReturn(OAuthProvider.LOCAL);
            lenient().when(user.getProviderId()).thenReturn(null);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            service.deleteMyAccount("alice");

            // 모든 cascade
            verify(ingredientRepository).deleteByUser(user);
            verify(recipeRepository).deleteByUser(user);
            verify(shoppingItemRepository).deleteByUser(user);
            verify(refreshTokenService).revokeAllForUser(user);
            verify(userRepository).delete(user);
            // 카카오는 호출 X
            verify(kakaoUnlinkClient, never()).unlink(any());
        }

        @Test
        @DisplayName("KAKAO 사용자 → cascade delete + Kakao unlink 호출")
        void kakaoUserUnlinks() {
            User user = mock(User.class);
            when(user.getProvider()).thenReturn(OAuthProvider.KAKAO);
            when(user.getProviderId()).thenReturn("kakao-12345");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            service.deleteMyAccount("alice");

            verify(userRepository).delete(user);
            verify(kakaoUnlinkClient).unlink("kakao-12345");
        }

        @Test
        @DisplayName("NAVER 사용자 → cascade delete만, Kakao unlink는 호출 안 됨 (네이버는 추후 작업)")
        void naverUserNoKakaoUnlink() {
            User user = mock(User.class);
            lenient().when(user.getProvider()).thenReturn(OAuthProvider.NAVER);
            lenient().when(user.getProviderId()).thenReturn("naver-678");
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            service.deleteMyAccount("alice");

            verify(userRepository).delete(user);
            verify(kakaoUnlinkClient, never()).unlink(any());
        }
    }

    @Nested
    @DisplayName("updateMyProfile")
    class UpdateMyProfile {

        @Test
        @DisplayName("사용자 없음 → IllegalArgumentException")
        void userNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMyProfile("ghost", validUpdate()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 update → updateProfile 호출 + 권장 칼로리 재계산")
        void recalculatesCalories() {
            User user = mock(User.class);
            // UserResponseDto 생성에 필요한 getter들 lenient 모킹
            lenient().when(user.getRole()).thenReturn(UserRole.USER);
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

            service.updateMyProfile("alice", validUpdate());

            verify(user).updateProfile(any(), any(), any(), any(), any(), any(), any(), any());
            verify(user).setRecommendedCalories(any(Integer.class));
        }

        private ProfileUpdateRequest validUpdate() {
            ProfileUpdateRequest req = new ProfileUpdateRequest();
            req.setName("홍길동");
            req.setGender("남");
            req.setBirthDate(LocalDate.of(1995, 5, 15));
            req.setHeight(175.0);
            req.setWeight(72.0);
            req.setActivityLevel("보통 활동");
            req.setDietGoal("체중 유지");
            req.setAllergies(null);
            return req;
        }
    }
}
