package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    }

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .filter(user -> {
                    // ✨ 로그인 방어벽 추가
                    if (user.isBanned()) {
                        throw new IllegalArgumentException("정지된 계정입니다. 관리자에게 문의하세요.");
                    }
                    return true;
                })
                .orElse(null);
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

        // 1. 사용자가 소유한 냉장고: 자식 데이터(초대/활동로그/식재료/멤버) 먼저 모두 정리 후 냉장고 삭제.
        //    같은 냉장고에 다른 가족 멤버가 있어도 함께 비워진다 (소유자 탈퇴 = 그 냉장고는 사라짐).
        for (Fridge fridge : fridgeRepository.findByOwner(user)) {
            fridgeInviteRepository.deleteByFridge(fridge);
            activityLogRepository.deleteByFridge(fridge);
            ingredientRepository.deleteByFridge(fridge);
            fridgeMemberRepository.deleteByFridge(fridge);
            fridgeRepository.delete(fridge);
        }

        // 2. 다른 사람 냉장고에 멤버로만 들어가 있던 흔적 정리.
        for (FridgeMember m : fridgeMemberRepository.findByUser(user)) {
            fridgeMemberRepository.delete(m);
        }

        // 3. 본인이 만든 데이터 일괄 정리. 1번에서 owned-fridge 한정 cleanup이 끝났으므로 여기는 잔여물(다른 냉장고에 추가한 식재료 등).
        ingredientRepository.deleteByUser(user);
        // 즐겨찾기 — 본인 것 + 본인이 만든 레시피를 다른 사람이 찜한 것 모두 정리해야 recipeRepository.deleteByUser 가 FK 위반 없이 동작.
        recipeFavoriteRepository.deleteByUser(user);
        recipeFavoriteRepository.deleteByRecipeOwner(user);
        recipeRepository.deleteByUser(user);
        shoppingItemRepository.deleteByUser(user);
        refreshTokenService.revokeAllForUser(user);
        fcmTokenRepository.deleteByUser(user);
        notificationRepository.deleteByUser(user);
        activityLogRepository.deleteByActor(user);
        userTokenRepository.deleteByUser(user);

        // 4. 모든 FK가 정리된 뒤 사용자 본체 삭제.
        userRepository.delete(user);

        if (provider == OAuthProvider.KAKAO && providerId != null) {
            kakaoUnlinkClient.unlink(providerId);
        }
    }
}