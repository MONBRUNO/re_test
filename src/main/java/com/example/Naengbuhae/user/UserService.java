package com.example.Naengbuhae.user;

import com.example.Naengbuhae.domain.Fridge;
import com.example.Naengbuhae.domain.FridgeMember;
import com.example.Naengbuhae.repository.FcmTokenRepository;
import com.example.Naengbuhae.repository.FridgeMemberRepository;
import com.example.Naengbuhae.repository.FridgeRepository;
import com.example.Naengbuhae.repository.IngredientRepository;
import com.example.Naengbuhae.repository.NotificationRepository;
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

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getUsername(), encodedPassword, UserRole.USER,
                request.getName(), request.getGender(), request.getHeight(),
                request.getWeight(), request.getBirthDate(), request.getEmail(),
                request.getActivityLevel(), request.getDietGoal(), request.getAllergies()
        );

        // 2. 칼로리 계산
        int calculatedCalories = CalorieCalculator.calculateRecommendedCalories(
                request.getGender(), request.getBirthDate(), request.getHeight(),
                request.getWeight(), request.getActivityLevel(), request.getDietGoal()
        );
        user.setRecommendedCalories(calculatedCalories);

        // 3. 사용자 저장
        User savedUser = userRepository.save(user);

        // 4. ✨ 파트너의 신규 기능 추가 (냉장고 생성 및 이메일 발송)
        Fridge defaultFridge = fridgeRepository.save(new Fridge(savedUser, defaultFridgeName(savedUser)));
        fridgeMemberRepository.save(new FridgeMember(defaultFridge, savedUser));
        emailAuthService.sendVerificationEmail(savedUser);
    }

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

    @Transactional
    public void deleteMyAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        OAuthProvider provider = user.getProvider();
        String providerId = user.getProviderId();

        ingredientRepository.deleteByUser(user);
        recipeRepository.deleteByUser(user);
        shoppingItemRepository.deleteByUser(user);
        refreshTokenService.revokeAllForUser(user);
        fcmTokenRepository.deleteByUser(user);
        notificationRepository.deleteByUser(user);
        userRepository.delete(user);

        if (provider == OAuthProvider.KAKAO && providerId != null) {
            kakaoUnlinkClient.unlink(providerId);
        }
    }
}