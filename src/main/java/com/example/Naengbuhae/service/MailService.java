package com.example.Naengbuhae.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

// 이메일 발송 헬퍼. JavaMailSender를 감싸서 HTML 메일 + 발신자 표시명 처리.
// SMTP 설정 (.env의 MAIL_USERNAME / MAIL_APP_PASSWORD)이 없으면 발송 시 IllegalStateException.
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    // 기본값은 application.properties의 유니코드 이스케이프(\\uB0C9\\uBD80\\uD574)에서 옴.
    // 여기 default 'naengbuhae'는 fallback일 뿐 실제로 적용될 일은 거의 없음.
    @Value("${app.mail.from-name:naengbuhae}")
    private String fromName;

    @Value("${app.mail.web-base-url:http://localhost:5173}")
    private String webBaseUrl;

    public String getWebBaseUrl() {
        return webBaseUrl;
    }

    // HTML 이메일 발송.
    @Async // ✨ 이제 비동기로 동작하여 DB 커넥션을 점유하지 않습니다!
    public void sendHtml(String to, String subject, String html) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME이 설정되지 않았습니다. .env 확인 필요.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            // 발신자 표시명을 명시적으로 MIME B-encoding ("=?UTF-8?B?...?=") 처리.
            // InternetAddress(addr, personal, charset)가 ASCII-only로 판단해 그냥 통과시키는 경우
            // (소스/env 인코딩 사고로 personal이 latin-1 영역으로 깨졌을 때)도 강제로 UTF-8 헤더 부착.
            String encodedFromName = MimeUtility.encodeText(fromName, "UTF-8", "B");
            helper.setFrom(new InternetAddress(encodedFromName + " <" + fromAddress + ">"));
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
