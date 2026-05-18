package com.example.Naengbuhae.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 감사 로그 전용 비동기 스레드 풀 설정
     * - corePoolSize: 기본적으로 유지되는 스레드 수
     * - maxPoolSize: 트래픽 급증 시 확장되는 최대 스레드 수
     * - queueCapacity: 스레드가 모두 사용 중일 때 대기하는 큐의 크기
     */
    @Bean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("AuditLog-Async-");
        executor.initialize();
        return executor;
    }
}
