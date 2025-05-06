package com.studybuddy.server.util;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class MailUtil {
    private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

    private static final String HOST = System.getenv("SMTP_HOST");
    private static final String USER = System.getenv("SMTP_USER");
    private static final String PASS = System.getenv("SMTP_PASS");

    private static final Session session;
    static {
        if (HOST == null || USER == null || PASS == null) {
            log.warn("SMTP 환경변수가 없으므로 메일 전송을 생략합니다 (dev mode)");
            session = null;
        } else {
            // 정상 환경변수가 모두 설정된 경우에만 Session 생성
            Properties p = new Properties();
            p.put("mail.smtp.host", HOST);
            p.put("mail.smtp.port", "587");
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.starttls.enable", "true");

            session = Session.getInstance(p, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USER, PASS);
                }
            });
        }
    }


    public static void sendCode(String to, String code) throws MessagingException {
        if (session == null) {
            log.warn("메일 세션이 초기화되지 않아 실제 전송을 하지 않습니다: to={}, code={}", to, code);
            return;
        }
        Message m = new MimeMessage(session);
        m.setFrom(new InternetAddress(USER));
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        m.setSubject("[StudyBuddy] 인증 코드");
        m.setText("인증 코드는 " + code + " (10분 이내 입력)");
        Transport.send(m);
        log.info("메일 전송 완료 → {} : code={}", to, code);
    }
}
