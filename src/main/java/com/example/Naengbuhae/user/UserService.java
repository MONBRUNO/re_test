package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.domain.enums.ActivityLevel;
import com.example.Naengbuhae.domain.enums.DietGoal;
import com.example.Naengbuhae.domain.enums.Gender;
import com.example.Naengbuhae.repository.ActivityLogRepository;
import com.example.Naengbuhae.repository.FcmTokenRepository;
import com.example.Naengbuhae.repository.FridgeInviteRepository;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.NotificationRepository;
import com.example.Naengbuhae.repository.RecipeFavoriteRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import com.example.Naengbuhae.util.CalorieCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final ShoppingItemRepository shoppingItemRepository;
    private final RefreshTokenService refreshTokenService;
    private final KakaoUnlinkClient kakaoUnlinkClient;
    // ✨ 파트너의 신규 의존성 유지
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final EmailAuthService emailAuthService;
    private final FcmTokenRepository fcmTokenRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final FridgeInviteRepository fridgeInviteRepository;
    private final UserTokenRepository userTokenRepository;
    private final RecipeFavoriteRepository recipeFavoriteRepository;

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::new)
                .toList(); // ✅ 원희 님의 최신 문법 유지
    }

    public long countUsers() {
        return userRepository.count();
    }

    public UserResponseDto getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserResponseDto(user);
    }

    @Transactional
    public UserResponseDto updateMyProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(
                request.getName(), request.getGender(), request.getHeight(),
                request.getWeight(), request.getBirthDate(), request.getActivityLevel(),
                request.getDietGoal(), request.getAllergies()
        );

        int recalculated = CalorieCalculator.calculateRecommendedCalories(
                request.getGender(), request.getBirthDate(), request.getHeight(),
                request.getWeight(), request.getActivityLevel(), request.getDietGoal()
        );
        user.setRecommendedCalories(recalculated);

        return new UserResponseDto(user);
    }

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

    @Transactional
    public void signup(SignupRequest request) { // ✅ 원희 님의 void 반환형 유지
        // 1. 유효성 검사 (원희 님의 Exception 방식 유지)
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (request.getPassword().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            throw new IllegalArgumentException("비밀번호에 한글은 사용할 수 없습니다.");
        }
        if (!request.getPassword().matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영어 소문자, 숫자, 특수문자를 포함해야 합니다.");
        }

        // 1-1. 가입 화면에서 받은 6자리 인증번호로 검증된 이메일만 통과. 동시에 verification row 정리.
        if (!emailAuthService.consumeVerifiedSignupCode(request.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getUsername(), encodedPassword, UserRole.USER,
                request.getName(), request.getGender(), request.getHeight(),
                request.getWeight(), request.getBirthDate(), request.getEmail(),
                request.getActivityLevel(), request.getDietGoal(), request.getAllergies()
        );
        // 인증 코드를 통과했으므로 verified 상태로 저장 — 매직 링크 메일 추가 발송 불필요.
        user.setEmailVerified(true);

        // 2. 칼로리 계산
        int calculatedCalories = CalorieCalculator.calculateRecommendedCalories(
                request.getGender(), request.getBirthDate(), request.getHeight(),
                request.getWeight(), request.getActivityLevel(), request.getDietGoal()
        );
        user.setRecommendedCalories(calculatedCalories);

        // 3. 사용자 저장 + 기본 냉장고
        User savedUser = userRepository.save(user);
        Fridge defaultFridge = fridgeRepository.save(new Fridge(savedUser, defaultFridgeName(savedUser)));
        fridgeMemberRepository.save(new FridgeMember(defaultFridge, savedUser));
    }

    public static String defaultFridgeName(User user) {
        String name = user == null ? null : user.getName();
        if (name == null || name.isBlank()) return "내 냉장고";
        return name + "의 냉장고";
    }

    // 로그인된 사용자의 비밀번호 변경. 현재 비번 일치 확인 + 새 비번 형식 검증 + 동일 비번 거부.
    // OAuth 가입자는 password가 placeholder라 의미 없으므로 막는다.
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (user.getProvider() != OAuthProvider.LOCAL) {
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호 변경을 지원하지 않아요.");
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }
        if (newPassword.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            throw new IllegalArgumentException("비밀번호에 한글은 사용할 수 없습니다.");
        }
        if (!newPassword.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며, 영어 소문자, 숫자, 특수문자를 포함해야 합니다.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("이전과 동일한 비밀번호입니다.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));

        // ✨ 보안 패치 3: 비밀번호 변경 시 기존의 모든 로그인 세션(리프레시 토큰)을 강제로 만료시킵니다.
        // 계정이 해킹당했을 경우 해커의 접속을 즉시 차단하기 위한 필수 조치입니다.
        refreshTokenService.revokeAllForUser(user);
    }

    @Transactional(noRollbackFor = {IllegalArgumentException.class, IllegalStateException.class})
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 사용자입니다."));

        // ✨ 보안 패치 1: 이 계정이 현재 잠겨있는지 확인
        if (user.getLockTime() != null) {
            if (user.getLockTime().isAfter(java.time.LocalDateTime.now())) {
                // 아직 잠금 시간이 안 지났음! 접근 거부!
                throw new IllegalStateException("비밀번호 5회 오류로 계정이 15분간 잠겼습니다. 나중에 다시 시도해주세요.");
            } else {
                // 시간이 지났으면 잠금 해제!
                user.resetFailedAttempt();
            }
        }

        // ✨ 보안 패치 2: 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.increaseFailedAttempt(); // 틀렸으니 실패 횟수 +1
            
            if (user.getFailedAttempt() >= 5) {
                // 5번 틀렸으면 지금부터 15분간 계정 잠금! 쾅!
                user.lockAccount(java.time.LocalDateTime.now().plusMinutes(15));
                userRepository.save(user); // 즉시 저장
                log.warn("🚨 [보안 경고] 계정 '{}'가 브루트 포스 공격 의심으로 15분간 잠금 처리되었습니다.", username);
                throw new IllegalStateException("비밀번호 5회 오류로 계정이 15분간 잠겼습니다. 나중에 다시 시도해주세요.");
            }
            
            userRepository.save(user); // 실패 횟수 저장
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다. (남은 기회: " + (5 - user.getFailedAttempt()) + "번)");
        }

        // ✨ 로그인 방어벽 추가 (기존 정지 계정 체크)
        if (user.isBanned()) {
            throw new IllegalArgumentException("정지된 계정입니다. 관리자에게 문의하세요.");
        }

        // ✨ 보안 패치 3: 비밀번호를 맞혔다면 실패 횟수 초기화!
        user.resetFailedAttempt();
        userRepository.save(user);
        
        return user;
    }

    @Transactional
    public void updateUserBanStatus(Long userId, boolean ban) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        // ✨ 관리자 팀킬 방지 로직 추가
        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("관리자 계정은 정지할 수 없습니다.");
        }
        
        // 1. 유저 상태 변경
        user.changeBanStatus(ban);
        
        // 2. ✨ 핵심 시니어 디테일: 유저를 정지(ban=true)하는 경우, 
        // 기존에 발급된 모든 리프레시 토큰을 제거하여 현재 로그인된 세션을 강제로 만료시킵니다.
        if (ban) {
            refreshTokenService.revokeAllForUser(user);
        }
    }

    @Transactional
    public void deleteMyAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        OAuthProvider provider = user.getProvider();
        String providerId = user.getProviderId();

        // 1. ✨ [N+1 박멸] 사용자가 소유한 냉장고 데이터 벌크 삭제
        //    하나씩 SELECT-DELETE 하던 비효율을 걷어내고, 관련 테이블을 쿼리 한 방씩으로 정리합니다.
        for (Fridge fridge : fridgeRepository.findByOwner(user)) {
            fridgeInviteRepository.deleteAllByFridgeInBatch(fridge);
            activityLogRepository.deleteAllByFridgeInBatch(fridge);
            ingredientRepository.deleteAllByFridgeInBatch(fridge);
            fridgeMemberRepository.deleteAllByFridgeInBatch(fridge);
            fridgeRepository.delete(fridge); // Fridge 본체 삭제
        }

        // 2. ✨ [성능 최적화] 다른 사람 냉장고에 멤버로 가입된 정보 정리 (벌크 삭제)
        fridgeMemberRepository.findByUser(user).forEach(m -> 
            fridgeMemberRepository.deleteByFridgeAndUserInBatch(m.getFridge(), user)
        );

        // 3. ✨ [보안 및 성능] 사용자가 생성한 모든 부속 데이터 벌크 삭제 (쿼리 폭탄 제거)
        ingredientRepository.deleteAllByUserInBatch(user);
        
        // 레시피 즐겨찾기: 본인이 한 것 + 내 레시피를 다른 사람이 한 것 한방에 정리
        recipeFavoriteRepository.deleteAllByUserInBatch(user);
        recipeFavoriteRepository.deleteAllByRecipeOwnerInBatch(user);
        
        // 레시피 본체 삭제 — 벌크 JPQL DELETE는 cascade를 타지 않아 자식 테이블
        // (recipe_ingredients, recipe_steps) row가 고아로 남아 FK 위반(409)이 난다.
        // 엔티티 단위 삭제로 cascade·orphanRemoval을 태워 자식까지 함께 정리한다.
        recipeRepository.deleteAll(recipeRepository.findByUser(user));
        
        shoppingItemRepository.deleteAllByUserInBatch(user);
        refreshTokenService.revokeAllForUser(user);
        fcmTokenRepository.deleteByUser(user); // FcmToken은 단일 row라 유지
        notificationRepository.deleteAllByUserInBatch(user);
        activityLogRepository.deleteAllByActorInBatch(user);
        userTokenRepository.deleteByUser(user);

        // 4. 모든 연관 데이터가 벌크로 정리된 후 최종 사용자 삭제
        userRepository.delete(user);

        if (provider == OAuthProvider.KAKAO && providerId != null) {
            kakaoUnlinkClient.unlink(providerId);
        }
    }
}