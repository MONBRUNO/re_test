package com.example.Naengbuhae.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.UnsupportedEncodingException;

// 이메일 발송 헬퍼. JavaMailSender를 감싸서 HTML 메일 + 발신자 표시명 처리.
// SMTP 설정 (.env의 MAIL_USERNAME / MAIL_APP_PASSWORD)이 없으면 발송 시 IllegalStateException.
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from:}")
    private String resendFrom;

    @Value("${gmail.api.client-id:}")
    private String gmailApiClientId;

    @Value("${gmail.api.client-secret:}")
    private String gmailApiClientSecret;

    @Value("${gmail.api.refresh-token:}")
    private String gmailApiRefreshToken;

    @Value("${gmail.api.from:}")
    private String gmailApiFrom;

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
    // Resend API 키가 있으면 HTTPS API를 우선 사용하고, 없으면 기존 Gmail SMTP를 fallback으로 사용한다.
    // 회원가입 인증 메일은 실패하면 사용자에게 즉시 알려야 하므로 동기로 처리한다.
    public void sendHtml(String to, String subject, String html) {
        if (hasGmailApiConfig()) {
            sendViaGmailApi(to, subject, html);
            return;
        }
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(to, subject, html);
            return;
        }
        sendViaSmtp(to, subject, html);
    }

    private void sendViaSmtp(String to, String subject, String html) {
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
            log.info("[MailService] SMTP 메일 발송 완료: to={}, subject={}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("SMTP 메일 발송 실패", e);
        }
    }

    private boolean hasGmailApiConfig() {
        return gmailApiClientId != null && !gmailApiClientId.isBlank()
                && gmailApiClientSecret != null && !gmailApiClientSecret.isBlank()
                && gmailApiRefreshToken != null && !gmailApiRefreshToken.isBlank()
                && gmailApiFrom != null && !gmailApiFrom.isBlank();
    }

    private void sendViaGmailApi(String to, String subject, String html) {
        try {
            String accessToken = requestGmailAccessToken();
            String rawMessage = createGmailRawMessage(to, subject, html);
            String body = objectMapper.writeValueAsString(Map.of("raw", rawMessage));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Gmail API 메일 발송 실패: status="
                        + response.statusCode() + ", body=" + response.body());
            }
            log.info("[MailService] Gmail API 메일 발송 완료: to={}, subject={}, response={}",
                    to, subject, response.body());
        } catch (IOException | MessagingException e) {
            throw new RuntimeException("Gmail API 메일 발송 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gmail API 메일 발송 중 인터럽트", e);
        }
    }

    private String requestGmailAccessToken() throws IOException, InterruptedException {
        String form = "client_id=" + urlEncode(gmailApiClientId)
                + "&client_secret=" + urlEncode(gmailApiClientSecret)
                + "&refresh_token=" + urlEncode(gmailApiRefreshToken)
                + "&grant_type=refresh_token";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Gmail API access token 발급 실패: status="
                    + response.statusCode() + ", body=" + response.body());
        }

        String accessToken = objectMapper.readTree(response.body()).path("access_token").asText();
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("Gmail API access token 응답이 비어 있습니다: " + response.body());
        }
        return accessToken;
    }

    private String createGmailRawMessage(String to, String subject, String html)
            throws MessagingException, IOException {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress(gmailApiFrom, fromName, "UTF-8"));
        message.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");
        message.setContent(html, "text/html; charset=UTF-8");
        message.saveChanges();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        message.writeTo(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void sendViaResend(String to, String subject, String html) {
        String from = (resendFrom != null && !resendFrom.isBlank())
                ? resendFrom
                : "Naengbuhae <onboarding@resend.dev>";
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", from,
                    "to", List.of(to),
                    "subject", subject,
                    "html", html
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Resend 메일 발송 실패: status="
                        + response.statusCode() + ", body=" + response.body());
            }
            log.info("[MailService] Resend 메일 발송 완료: to={}, subject={}, response={}",
                    to, subject, response.body());
        } catch (IOException e) {
            throw new RuntimeException("Resend 메일 발송 요청 생성 실패", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Resend 메일 발송 중 인터럽트", e);
        }
    }
}
