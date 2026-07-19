package com.fschool.edu.fschool_backend.infrastructure.mail;

import com.fschool.edu.fschool_backend.presentation.exception.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OtpMailService {

    private static final Logger log = LoggerFactory.getLogger(OtpMailService.class);
    private static final String FORGOT_PASSWORD_SUBJECT = "Ma OTP dat lai mat khau FSchool";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public void sendForgotPasswordOtp(String recipientEmail, String recipientName, String otp, long ttlSeconds) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!hasText(mailHost) || mailSender == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Chua cau hinh SMTP de gui OTP");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(recipientEmail);
            String fromAddress = fromAddress();
            if (hasText(fromAddress)) {
                helper.setFrom(fromAddress);
            }
            helper.setSubject(FORGOT_PASSWORD_SUBJECT);
            helper.setText(emailBody(recipientName, otp, ttlSeconds), false);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            log.warn("Cannot send forgot password OTP email to {}", recipientEmail, exception);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Khong the gui email OTP, vui long kiem tra cau hinh SMTP");
        }
    }

    private String emailBody(String recipientName, String otp, long ttlSeconds) {
        String displayName = hasText(recipientName) ? recipientName.trim() : "ban";
        long ttlMinutes = Math.max(1, (ttlSeconds + 59) / 60);
        return """
                Xin chao %s,

                Ma OTP dat lai mat khau cua ban la: %s

                Ma co hieu luc trong %d phut. Vui long khong chia se ma nay voi bat ky ai.

                FSchool
                """.formatted(displayName, otp, ttlMinutes);
    }

    private String fromAddress() {
        if (hasText(mailFrom)) {
            return mailFrom.trim();
        }
        if (hasText(mailUsername)) {
            return mailUsername.trim();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
