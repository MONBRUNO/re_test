/*
package com.example.Naengbuhae.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String signup(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return "아이디를 입력해주세요.";
        }

        if (password == null || password.trim().isEmpty()) {
            return "비밀번호를 입력해주세요.";
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
        return "회원가입 성공";
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (password == null || password.trim().isEmpty()) {
            return null;
        }

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }
}

 */


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

    // 강력한 비밀번호 검증 규칙 (영문 소문자, 숫자, 특수문자 포함 8자 이상)
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

    public String signup(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return "아이디를 입력해주세요.";
        }
        if (password == null || password.trim().isEmpty()) {
            return "비밀번호를 입력해주세요.";
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }
        if (password.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣].*")) {
            return "비밀번호에 한글은 사용할 수 없습니다.";
        }
        if (!password.matches(PASSWORD_REGEX)) {
            return "비밀번호는 8자 이상이며, 영어 소문자, 숫자, 특수문자를 포함해야 합니다.";
        }

        // 비밀번호 암호화 후 엔티티 생성
        String encodedPassword = passwordEncoder.encode(password);
        
        // 권한 결정 (관리자 백도어 로직)
        UserRole role = UserRole.USER;
        if (username.equals("admin")) {
            role = UserRole.ADMIN;
        }

        User user = new User(username, encodedPassword, role);

        userRepository.save(user);
        return "회원가입 성공";
    }

    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        if (password == null || password.trim().isEmpty()) {
            return null;
        }

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .orElse(null);
    }
}
