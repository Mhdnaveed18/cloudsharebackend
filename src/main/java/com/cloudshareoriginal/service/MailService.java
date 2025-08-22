package com.cloudshareoriginal.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@example.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:Support}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String mailUsername; // fallback for from address

    private String resolveFromEmail() {
        if (fromEmail == null || fromEmail.isBlank() || "no-reply@example.com".equalsIgnoreCase(fromEmail)) {
            return (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : "no-reply@example.com";
        }
        return fromEmail;
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(to);
            helper.setFrom(new InternetAddress(resolveFromEmail(), fromName));
            helper.setSubject("Password Reset Request");

            String html = "<p>We received a request to reset your password.</p>"
                    + "<p>Click the button below to reset your password. This link expires soon.</p>"
                    + "<p><a href=\"" + resetLink + "\" "
                    + "style=\"background:#1a73e8;color:#fff;padding:10px 16px;text-decoration:none;"
                    + "border-radius:6px;display:inline-block\">Reset Password</a></p>"
                    + "<p>If you did not request this, you can ignore this email.</p>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Error sending password reset email to {}", to, ex);
        }
    }

    public void sendEmailVerification(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
            helper.setTo(to);
            helper.setFrom(new InternetAddress(resolveFromEmail(), fromName));
            helper.setSubject("Verify Your Email");

            String html = "<p>Welcome! Please verify your email address.</p>"
                    + "<p>Your verification code is:</p>"
                    + "<p style=\"font-size:20px;font-weight:bold;letter-spacing:2px;\">" + code + "</p>"
                    + "<p>This code will expire soon. If you did not register, you can ignore this email.</p>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Error sending verification email to {}", to, ex);
        }
    }
}
