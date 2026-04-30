package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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

    // 강력한 비밀번호 검증 규칙 (영문 소문자, 숫자, 특수문자 포함 8자 이상)
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

    public String signup(SignupRequest request) {
        // [비즈니스 검증] 1. 아이디 및 이메일 중복 체크 (DB 조회 필수)
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "이미 사용 중인 이메일입니다.";
        }

        // [비즈니스 검증] 2. 특수 비밀번호 정책 체크
        if (request.getPassword().matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            return "비밀번호에 한글은 사용할 수 없습니다.";
        }
        if (!request.getPassword().matches(PASSWORD_REGEX)) {
            return "비밀번호는 8자 이상이며, 영어 소문자, 숫자, 특수문자를 포함해야 합니다.";
        }

        // 비밀번호 암호화 후 엔티티 생성
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 권한 결정 (모든 신규 가입자는 USER 권한을 가짐 - 관리자 백도어 원천 차단)
        UserRole role = UserRole.USER;

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

        userRepository.save(user);
        return "회원가입 성공";
    }

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }
}