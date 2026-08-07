package shopstack_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // The base URL of your React app's reset-password page, e.g.
    // http://localhost:5173/reset-password
    @Value("${app.frontend-reset-url}")
    private String frontendResetUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String token) {

        String resetLink = frontendResetUrl + "?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("ShopStack — Reset Your Password");
        message.setText(
                "We received a request to reset your ShopStack password.\n\n" +
                "Click the link below to choose a new password. This link " +
                "expires in 30 minutes:\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email."
        );

        mailSender.send(message);
    }
}