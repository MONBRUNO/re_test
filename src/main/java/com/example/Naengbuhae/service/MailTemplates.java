package com.example.Naengbuhae.service;

// 인라인 HTML 메일 템플릿. CSS는 inline style만 (메일 클라이언트 호환성).
public final class MailTemplates {

    private MailTemplates() {}

    private static final String COLOR_ACCENT = "#CDFF00";
    private static final String COLOR_TEXT = "#111827";
    private static final String COLOR_MUTED = "#6B7280";

    public static String verifyEmail(String userName, String verifyUrl) {
        return wrap(
                "이메일 인증",
                userName + "님, 회원가입을 환영합니다!",
                "아래 버튼을 눌러 이메일 인증을 완료해주세요. 인증 후 모든 기능을 자유롭게 사용할 수 있습니다.",
                "이메일 인증하기",
                verifyUrl,
                "이 링크는 24시간 동안 유효합니다."
        );
    }

    // 가입 화면에서 인라인으로 입력할 6자리 코드 메일. 링크 X.
    public static String verifyEmailCode(String code) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',-apple-system,sans-serif;color:" + COLOR_TEXT + ";\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;background:#f5f5f5;padding:40px 20px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;\">"
                + "<tr><td style=\"padding:32px;\">"
                + "<h1 style=\"margin:0;font-size:22px;font-weight:700;\">냉부해 - 이메일 인증</h1>"
                + "<p style=\"margin:16px 0 0;font-size:14px;line-height:1.6;\">아래 6자리 인증번호를 회원가입 화면에 입력해주세요.</p>"
                + "<div style=\"margin:24px 0;padding:20px;background:" + COLOR_ACCENT + ";border-radius:12px;text-align:center;\">"
                + "<span style=\"font-size:32px;font-weight:700;letter-spacing:8px;color:" + COLOR_TEXT + ";\">" + escape(code) + "</span>"
                + "</div>"
                + "<p style=\"margin:16px 0 0;font-size:12px;color:" + COLOR_MUTED + ";\">이 코드는 10분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px;background:#fafafa;font-size:11px;color:" + COLOR_MUTED + ";text-align:center;\">"
                + "이 메일은 발신 전용입니다."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    public static String passwordReset(String userName, String resetUrl) {
        return wrap(
                "비밀번호 재설정",
                userName + "님, 비밀번호 재설정 요청을 받았어요.",
                "아래 버튼을 눌러 새 비밀번호를 설정해주세요. 본인이 요청하지 않았다면 이 메일을 무시해주세요.",
                "비밀번호 재설정하기",
                resetUrl,
                "이 링크는 30분 동안 유효합니다."
        );
    }

    private static String wrap(String subject, String greeting, String body, String buttonLabel,
                               String buttonUrl, String footer) {
        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f5f5f5;font-family:'Apple SD Gothic Neo',-apple-system,sans-serif;color:" + COLOR_TEXT + ";\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;background:#f5f5f5;padding:40px 20px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;\">"
                + "<tr><td style=\"padding:32px 32px 16px;\">"
                + "<h1 style=\"margin:0;font-size:22px;font-weight:700;color:" + COLOR_TEXT + ";\">냉부해 - " + escape(subject) + "</h1>"
                + "<p style=\"margin:16px 0 0;font-size:14px;color:" + COLOR_TEXT + ";line-height:1.6;\">" + escape(greeting) + "</p>"
                + "<p style=\"margin:8px 0 24px;font-size:13px;color:" + COLOR_MUTED + ";line-height:1.6;\">" + escape(body) + "</p>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 auto;\"><tr><td>"
                + "<a href=\"" + buttonUrl + "\" style=\"display:inline-block;padding:14px 28px;background:" + COLOR_ACCENT + ";color:" + COLOR_TEXT + ";font-weight:600;font-size:14px;text-decoration:none;border-radius:10px;\">" + escape(buttonLabel) + "</a>"
                + "</td></tr></table>"
                + "<p style=\"margin:24px 0 0;font-size:12px;color:" + COLOR_MUTED + ";\">버튼이 안 보이면 아래 링크를 복사해 브라우저에 붙여넣어주세요:</p>"
                + "<p style=\"margin:4px 0 0;font-size:12px;color:" + COLOR_MUTED + ";word-break:break-all;\">" + buttonUrl + "</p>"
                + "<p style=\"margin:24px 0 0;font-size:11px;color:" + COLOR_MUTED + ";\">" + escape(footer) + "</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px;background:#fafafa;font-size:11px;color:" + COLOR_MUTED + ";text-align:center;\">"
                + "이 메일은 발신 전용입니다."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
