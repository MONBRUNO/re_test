package com.example.Naengbuhae.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 추가: 비밀번호 암호화 클래스
import org.springframework.security.crypto.password.PasswordEncoder; // 추가: 암호화 인터페이스
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; //CORS 에러 방어막
import org.springframework.web.cors.CorsConfigurationSource; //CORS 에러 방어막
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; //CORS 에러 방어막
import java.util.List; //CORS 에러 방어막

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // 암호화 빈(Bean)! 서버 에러 방지용!
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception { // 주의: http.build() 때문에 throws Exception이 있어야 해!
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth // 이 코드 수정 이유는 스웨거(Swagger) 메뉴판이랑 호환이 안 되어서..
                        // 로그인, 회원가입 + 스웨거 관련 주소는 신분증 없이 프리패스!
                        .requestMatchers(
                                "/user/signup",
                                "/user/login",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                //"/api/ingredients", // 테스트를 위해 임시로 만든 통로2
                                //"/api/ingredients/**",// 테스트를 위해 임시로 만든 통로
                                "/error" //에러를 출력하기 위한
                        ).permitAll()
                        .anyRequest().authenticated() // 나머지는 다 신분증(JWT) 검사해!
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    // 🛡️ CORS 에러 방어막: "우리 프론트엔드가 보내는 요청은 다 받아줘!"
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 🚨 프론트엔드가 켜진 포트 번호(보통 3000 또는 5173)를 적어줘야 해!
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // JWT 토큰을 주고받으려면 이거 꼭 true여야 해!

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 주소(/**)에 이 규칙을 적용!
        return source;
    }
}