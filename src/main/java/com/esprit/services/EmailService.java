package com.esprit.services;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.esprit.utils.EmailConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {

    // Thread pool for handling email sending in background
    private static final Executor emailExecutor = Executors.newFixedThreadPool(2);

    /**
     * Asynchronously sends an email on a background thread
     * 
     * @param to      Recipient email
     * @param subject Email subject
     * @param content Email content (HTML)
     * @return CompletableFuture that completes with true if sent successfully,
     *         false otherwise
     */
    public CompletableFuture<Boolean> sendEmailAsync(String to, String subject, String content) {
        return CompletableFuture.supplyAsync(() -> {
            return sendEmailInternal(to, subject, content);
        }, emailExecutor);
    }

    /**
     * Synchronously sends an email (for backwards compatibility)
     * 
     * @param to      Recipient email
     * @param subject Email subject
     * @param content Email content (HTML)
     * @return True if sent successfully
     */
    public boolean sendEmail(String to, String subject, String content) {
        // Start async operation but wait for result
        try {
            return sendEmailAsync(to, subject, content).join();
        } catch (Exception e) {
            System.out.println("Sync email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Internal method that actually sends the email
     */
    private boolean sendEmailInternal(String to, String subject, String content) {
        try {
            // Get email configuration
            Properties props = new Properties();
            props.put("mail.smtp.auth", EmailConfig.getProperties().getProperty("mail.smtp.auth"));
            props.put("mail.smtp.host", EmailConfig.getProperties().getProperty("mail.smtp.host"));
            props.put("mail.smtp.port", EmailConfig.getProperties().getProperty("mail.smtp.port"));
            props.put("mail.smtp.starttls.enable",
                    EmailConfig.getProperties().getProperty("mail.smtp.starttls.enable"));

            // Create session with authentication
            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            EmailConfig.getUsername(),
                            EmailConfig.getPassword());
                }
            });

            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EmailConfig.getFromEmail()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");

            // Send message
            Transport.send(message);
            System.out.println("Email sent successfully to: " + to);
            return true;
        } catch (Exception e) {
            System.out.println("Email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a verification email to a user asynchronously
     * 
     * @param email User's email
     * @param name  User's name
     * @param token Verification token
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendVerificationEmailAsync(String email, String name, String code) {
        String subject = "Your UNICLUBS Verification Code";

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #00A0E3;'>Welcome to UNICLUBS!</h2>" +
                "<p>Hello " + name + ",</p>" +
                "<p>Thank you for creating an account. To verify your email address, please use the following verification code:</p>" +
                "<div style='background-color: #f4f4f4; padding: 20px; margin: 20px 0; text-align: center; border-radius: 5px;'>" +
                "<h2 style='margin: 0; color: #00A0E3; font-size: 32px; letter-spacing: 5px;'>" + code + "</h2>" +
                "</div>" +
                "<p>This verification code will expire in <strong>2 hours</strong>.</p>" +
                "<p>If you did not request this code, please ignore this email.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendVerificationEmail(String email, String name, String code) {
        try {
            return sendVerificationEmailAsync(email, name, code).join();
        } catch (Exception e) {
            System.out.println("Sync verification email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a password reset email to a user asynchronously
     * 
     * @param email User's email
     * @param name  User's name
     * @param token Password reset token
     * @return CompletableFuture that completes with true if sent successfully
     */
    public CompletableFuture<Boolean> sendPasswordResetEmailAsync(String email, String name, String token) {
        String subject = "Your UNICLUBS Password Reset Code";

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #00A0E3;'>Password Reset</h2>" +
                "<p>Hello " + name + ",</p>" +
                "<p>We received a request to reset your password. To proceed, please use the following code:</p>" +
                "<div style='background-color: #f4f4f4; padding: 20px; margin: 20px 0; text-align: center; border-radius: 5px;'>" +
                "<h2 style='margin: 0; color: #00A0E3; font-size: 32px; letter-spacing: 5px;'>" + token + "</h2>" +
                "</div>" +
                "<p>This reset code will expire in <strong>2 hours</strong>.</p>" +
                "<p>If you did not request this code, please ignore this email or contact support if you have concerns.</p>" +
                "<p style='margin-top: 30px; padding-top: 15px; border-top: 1px solid #eee; font-size: 12px; color: #666;'>" +
                "This is an automated message, please do not reply. If you need assistance, please contact support.</p>" +
                "</div>";

        return sendEmailAsync(email, subject, content);
    }

    /**
     * Synchronous version for backward compatibility
     */
    public boolean sendPasswordResetEmail(String email, String name, String token) {
        try {
            return sendPasswordResetEmailAsync(email, name, token).join();
        } catch (Exception e) {
            System.out.println("Sync password reset email send failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static EmailService getInstance() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getInstance'");
    }
}
