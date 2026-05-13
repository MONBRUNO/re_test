package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.RecipeRepository;
import com.example.Naengbuhae.repository.ShoppingItemRepository;
import com.example.Naengbuhae.util.CalorieCalculator; // ✅ 1. 계산기 임포트 추가!
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private final FridgeRepository fridgeRepository;
    private final FridgeMemberRepository fridgeMemberRepository;
    private final EmailAuthService emailAuthService;

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    public long countUsers() {
        return userRepository.count();
    }

    // 현재 로그인한 사용자의 프로필 조회 (프론트 MyCustom 페이지용)
    public UserResponseDto getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserResponseDto(user);
    }

    // 프로필 수정 + 신체정보 기반 권장 칼로리 자동 재계산
    @Transactional
    public UserResponseDto updateMyProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(
                request.getName(),
                request.getGender(),
                request.getHeight(),
                request.getWeight(),
                request.getBirthDate(),
                request.getActivityLevel(),
                request.getDietGoal(),
                request.getAllergies()
        );

        // 신체정보가 바뀔 가능성이 항상 있으므로 권장 칼로리 재계산
        int recalculated = CalorieCalculator.calculateRecommendedCalories(
                request.getGender(),
                request.getBirthDate(),
                request.getHeight(),
                request.getWeight(),
                request.getActivityLevel(),
                request.getDietGoal()
        );
        user.setRecommendedCalories(recalculated);

        return new UserResponseDto(user);
    }

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

    @Transactional
    public String signup(SignupRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "이미 사용 중인 이메일입니다.";
        }

        if (request.getPassword().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            return "비밀번호에 한글은 사용할 수 없습니다.";
        }
        if (!request.getPassword().matches(PASSWORD_REGEX)) {
            return "비밀번호는 8자 이상이며, 영어 소문자, 숫자, 특수문자를 포함해야 합니다.";
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        UserRole role = UserRole.USER;

        // 1. 기존 방식대로 User 객체 먼저 생성
        User user = new User(
                request.getUsername(),
                encodedPassword,
                role,
                request.getName(),
                request.getGender(),
                request.getHeight(),
                request.getWeight(),
                request.getBirthDate(),
                request.getEmail(),
                request.getActivityLevel(),
                request.getDietGoal(),
                request.getAllergies()
        );

        // ✅ 2. 여기서 칼로리 계산기 호출!
        int calculatedCalories = CalorieCalculator.calculateRecommendedCalories(
                request.getGender(),
                request.getBirthDate(),
                request.getHeight(),
                request.getWeight(),
                request.getActivityLevel(),
                request.getDietGoal()
        );

        // ✅ 3. 계산된 칼로리를 User 객체에 쏙 넣어주기!
        user.setRecommendedCalories(calculatedCalories);

        // 4. DB에 최종 저장
        User savedUser = userRepository.save(user);

        // 5. 신규 가입자에게 "<이름>의 냉장고" 자동 생성 — 친구 초대 시 누구 냉장고인지 식별 쉽게.
        //    여러 냉장고 관리(김치냉장고 등)는 사용자가 원할 때 추가.
        Fridge defaultFridge = fridgeRepository.save(new Fridge(savedUser, defaultFridgeName(savedUser)));
        fridgeMemberRepository.save(new FridgeMember(defaultFridge, savedUser));

        // 6. 이메일 인증 메일 발송. 실패해도 가입 자체는 성공 처리 (마이페이지에서 재발송 가능).
        emailAuthService.sendVerificationEmail(savedUser);

        return "회원가입 성공";
    }

    // 회원 이름 기반 기본 냉장고 이름. 가족 공유 시 누구 냉장고인지 식별 쉽게.
    // 이름이 비어있으면 fallback으로 "내 냉장고".
    public static String defaultFridgeName(User user) {
        String name = user == null ? null : user.getName();
        if (name == null || name.isBlank()) return "내 냉장고";
        return name + "의 냉장고";
    }

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }

    // 회원 탈퇴: 사용자가 만든 식재료/레시피/장보기 목록까지 모두 삭제 + OAuth 제공자 연결 해제
    @Transactional
    public void deleteMyAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 트랜잭션 롤백 시 unlink 호출이 부적절해지지 않도록 provider 정보를 먼저 스냅샷
        OAuthProvider provider = user.getProvider();
        String providerId = user.getProviderId();

        ingredientRepository.deleteByUser(user);
        recipeRepository.deleteByUser(user);
        shoppingItemRepository.deleteByUser(user);
        refreshTokenService.revokeAllForUser(user);
        userRepository.delete(user);

        // best-effort: 카카오 연결 해제. 실패해도 탈퇴는 이미 진행되므로 예외를 던지지 않음.
        // 구글/네이버는 사용자 access token이 필요한데 저장하지 않으므로 추후 작업.
        if (provider == OAuthProvider.KAKAO && providerId != null) {
            kakaoUnlinkClient.unlink(providerId);
        }
    }
}