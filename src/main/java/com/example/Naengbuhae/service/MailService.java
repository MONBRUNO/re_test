package com.example.Naengbuhae.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

// 이메일 발송 헬퍼. JavaMailSender를 감싸서 HTML 메일 + 발신자 표시명 처리.
// SMTP 설정 (.env의 MAIL_USERNAME / MAIL_APP_PASSWORD)이 없으면 발송 시 IllegalStateException.
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.mail.from-name:냉부해}")
    private String fromName;

    @Value("${app.mail.web-base-url:http://localhost:5173}")
    private String webBaseUrl;

    public String getWebBaseUrl() {
        return webBaseUrl;
    }

    // HTML 이메일 발송.
    public void sendHtml(String to, String subject, String html) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME이 설정되지 않았습니다. .env 확인 필요.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // HTML
            mailSender.send(message);
            log.info("[MailService] 메일 발송 완료: to={}, subject={}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
}
