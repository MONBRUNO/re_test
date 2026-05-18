package com.example.Naengbuhae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // ✨ import 추가
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync // ✨ 비동기 기능 활성화!
@EnableScheduling
@SpringBootApplication
public class NaengbuhaeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NaengbuhaeApplication.class, args);
    }

}