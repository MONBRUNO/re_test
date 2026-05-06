package com.example.Naengbuhae.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// SecurityConfig에서 분리: SecurityConfig가 CustomOAuth2UserService를 의존하고,
// CustomOAuth2UserService가 PasswordEncoder를 의존하면 순환 참조가 발생하므로
// PasswordEncoder는 별도 설정 클래스에서 정의한다.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
