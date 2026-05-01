package com.example.Naengbuhae.user;

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

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    public long countUsers() {
        return userRepository.count();
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
        userRepository.save(user);
        return "회원가입 성공";
    }

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }
}