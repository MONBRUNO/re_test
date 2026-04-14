package org.example.re_test.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";

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

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(username, encodedPassword);

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